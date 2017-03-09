/*
 *      Copyright (C) 2012-2017 DataStax Inc.
 *
 *      This software can be used solely with DataStax Enterprise. Please consult the license at
 *      http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.driver.mapping;

import com.datastax.driver.core.*;
import com.datastax.driver.mapping.MethodMapper.ParamMapper;
import com.datastax.driver.mapping.annotations.*;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Static methods that facilitates parsing:
 * - {@link #parseEntity(Class, MappingManager)}: entity classes into {@link EntityMapper} instances
 * - {@link #parseUDT(Class, MappingManager)}: UDT classes into {@link MappedUDTCodec} instances.
 * - {@link #parseAccessor(Class, MappingManager)}: Accessor interfaces into {@link AccessorMapper} instances.
 */
class AnnotationParser {

    /**
     * Annotations allowed on a property that maps to a table column.
     */
    @SuppressWarnings("unchecked")
    private static final Set<Class<? extends Annotation>> VALID_COLUMN_ANNOTATIONS = ImmutableSet.of(
            Column.class,
            Computed.class,
            ClusteringColumn.class,
            Frozen.class,
            FrozenKey.class,
            FrozenValue.class,
            PartitionKey.class,
            Transient.class);

    /**
     * Annotations allowed on a property that maps to a UDT field.
     */
    private static final Set<Class<? extends Annotation>> VALID_FIELD_ANNOTATIONS = ImmutableSet.of(
            Field.class,
            Frozen.class,
            FrozenKey.class,
            FrozenValue.class,
            Transient.class);

    private static final Comparator<PropertyMapper> POSITION_COMPARATOR = new Comparator<PropertyMapper>() {
        @Override
        public int compare(PropertyMapper o1, PropertyMapper o2) {
            return o1.position - o2.position;
        }
    };

    private AnnotationParser() {
    }

    static <T> EntityMapper<T> parseEntity(final Class<T> entityClass, MappingManager mappingManager) {
        Table table = AnnotationChecks.getTypeAnnotation(Table.class, entityClass);

        String ksName = table.caseSensitiveKeyspace() ? Metadata.quote(table.keyspace()) : table.keyspace().toLowerCase();
        String tableName = table.caseSensitiveTable() ? Metadata.quote(table.name()) : table.name().toLowerCase();

        ConsistencyLevel writeConsistency = table.writeConsistency().isEmpty() ? null : ConsistencyLevel.valueOf(table.writeConsistency().toUpperCase());
        ConsistencyLevel readConsistency = table.readConsistency().isEmpty() ? null : ConsistencyLevel.valueOf(table.readConsistency().toUpperCase());

        if (Strings.isNullOrEmpty(table.keyspace())) {
            String loggedKeyspace = mappingManager.getSession().getLoggedKeyspace();
            if (Strings.isNullOrEmpty(loggedKeyspace))
                throw new IllegalArgumentException(String.format(
                        "Error creating mapper for %s, the @Table annotation declares no default keyspace, and the session is not currently logged to any keyspace",
                        entityClass
                ));
            ksName = Metadata.quote(loggedKeyspace);
        }

        KeyspaceMetadata keyspaceMetadata = mappingManager.getSession().getCluster().getMetadata().getKeyspace(ksName);
        if (keyspaceMetadata == null)
            throw new IllegalArgumentException(String.format("Keyspace %s does not exist", ksName));

        AbstractTableMetadata tableMetadata = keyspaceMetadata.getTable(tableName);
        if (tableMetadata == null) {
            tableMetadata = keyspaceMetadata.getMaterializedView(tableName);
            if (tableMetadata == null)
                throw new IllegalArgumentException(String.format("Table or materialized view %s does not exist in keyspace %s", tableName, ksName));
        }

        EntityMapper<T> mapper = new EntityMapper<T>(entityClass, ksName, tableName, writeConsistency, readConsistency);

        List<PropertyMapper> pks = new ArrayList<PropertyMapper>();
        List<PropertyMapper> ccs = new ArrayList<PropertyMapper>();
        List<PropertyMapper> rgs = new ArrayList<PropertyMapper>();

        Map<String, Object[]> fieldsAndProperties = ReflectionUtils.scanFieldsAndProperties(entityClass);
        AtomicInteger columnCounter = mappingManager.isCassandraV1 ? null : new AtomicInteger(0);

        for (Map.Entry<String, Object[]> entry : fieldsAndProperties.entrySet()) {

            String propertyName = entry.getKey();
            java.lang.reflect.Field field = (java.lang.reflect.Field) entry.getValue()[0];
            PropertyDescriptor property = (PropertyDescriptor) entry.getValue()[1];
            String alias = (columnCounter != null)
                    ? "col" + columnCounter.incrementAndGet()
                    : null;

            PropertyMapper propertyMapper = new PropertyMapper(entityClass, propertyName, alias, field, property);

            if (mappingManager.isCassandraV1 && propertyMapper.isComputed())
                throw new UnsupportedOperationException("Computed properties are not supported with native protocol v1");

            AnnotationChecks.validateAnnotations(propertyMapper, VALID_COLUMN_ANNOTATIONS);

            if (propertyMapper.isTransient())
                continue;

            if (!propertyMapper.isComputed() && tableMetadata.getColumn(propertyMapper.columnName) == null)
                throw new IllegalArgumentException(String.format("Column %s does not exist in table %s.%s",
                        propertyMapper.columnName, ksName, tableName));

            if (propertyMapper.isPartitionKey())
                pks.add(propertyMapper);
            else if (propertyMapper.isClusteringColumn())
                ccs.add(propertyMapper);
            else
                rgs.add(propertyMapper);

            // if the property is of a UDT type, parse it now
            for (Class<?> udt : TypeMappings.findUDTs(propertyMapper.javaType.getType()))
                mappingManager.getUDTCodec(udt);
        }

        Collections.sort(pks, POSITION_COMPARATOR);
        Collections.sort(ccs, POSITION_COMPARATOR);

        AnnotationChecks.validateOrder(pks, "@PartitionKey");
        AnnotationChecks.validateOrder(ccs, "@ClusteringColumn");

        mapper.addColumns(pks, ccs, rgs);
        return mapper;
    }

    static <T> MappedUDTCodec<T> parseUDT(Class<T> udtClass, MappingManager mappingManager) {
        UDT udt = AnnotationChecks.getTypeAnnotation(UDT.class, udtClass);

        String ksName = udt.caseSensitiveKeyspace() ? Metadata.quote(udt.keyspace()) : udt.keyspace().toLowerCase();
        String udtName = udt.caseSensitiveType() ? Metadata.quote(udt.name()) : udt.name().toLowerCase();

        if (Strings.isNullOrEmpty(udt.keyspace())) {
            String loggedKeyspace = mappingManager.getSession().getLoggedKeyspace();
            if (Strings.isNullOrEmpty(loggedKeyspace))
                throw new IllegalArgumentException(String.format(
                        "Error creating UDT codec for %s, the @UDT annotation declares no default keyspace, and the session is not currently logged to any keyspace",
                        udtClass
                ));
            ksName = Metadata.quote(loggedKeyspace);
        }

        KeyspaceMetadata keyspaceMetadata = mappingManager.getSession().getCluster().getMetadata().getKeyspace(ksName);
        if (keyspaceMetadata == null)
            throw new IllegalArgumentException(String.format("Keyspace %s does not exist", ksName));

        UserType userType = keyspaceMetadata.getUserType(udtName);
        if (userType == null)
            throw new IllegalArgumentException(String.format("User type %s does not exist in keyspace %s", udtName, ksName));

        Map<String, PropertyMapper> propertyMappers = new HashMap<String, PropertyMapper>();

        Map<String, Object[]> fieldsAndProperties = ReflectionUtils.scanFieldsAndProperties(udtClass);

        for (Map.Entry<String, Object[]> entry : fieldsAndProperties.entrySet()) {

            String propertyName = entry.getKey();
            java.lang.reflect.Field field = (java.lang.reflect.Field) entry.getValue()[0];
            PropertyDescriptor property = (PropertyDescriptor) entry.getValue()[1];

            PropertyMapper propertyMapper = new PropertyMapper(udtClass, propertyName, null, field, property);

            AnnotationChecks.validateAnnotations(propertyMapper, VALID_FIELD_ANNOTATIONS);

            if (propertyMapper.isTransient())
                continue;

            if (!userType.contains(propertyMapper.columnName))
                throw new IllegalArgumentException(String.format("Field %s does not exist in type %s.%s",
                        propertyMapper.columnName, ksName, userType.getTypeName()));

            for (Class<?> fieldUdt : TypeMappings.findUDTs(propertyMapper.javaType.getType()))
                mappingManager.getUDTCodec(fieldUdt);

            propertyMappers.put(propertyMapper.columnName, propertyMapper);
        }

        return new MappedUDTCodec<T>(userType, udtClass, propertyMappers, mappingManager);
    }

    static <T> AccessorMapper<T> parseAccessor(Class<T> accClass, MappingManager mappingManager) {
        if (!accClass.isInterface())
            throw new IllegalArgumentException("@Accessor annotation is only allowed on interfaces, got " + accClass);

        AnnotationChecks.getTypeAnnotation(Accessor.class, accClass);

        List<MethodMapper> methods = new ArrayList<MethodMapper>();
        for (Method m : accClass.getDeclaredMethods()) {
            Query query = m.getAnnotation(Query.class);
            if (query == null)
                continue;

            String queryString = query.value();

            Annotation[][] paramAnnotations = m.getParameterAnnotations();
            Type[] paramTypes = m.getGenericParameterTypes();
            ParamMapper[] paramMappers = new ParamMapper[paramAnnotations.length];
            Boolean allParamsNamed = null;
            for (int i = 0; i < paramMappers.length; i++) {
                String paramName = null;
                Class<? extends TypeCodec<?>> codecClass = null;
                for (Annotation a : paramAnnotations[i]) {
                    if (a.annotationType().equals(Param.class)) {
                        Param param = (Param) a;
                        paramName = param.value();
                        if (paramName.isEmpty())
                            paramName = null;
                        codecClass = param.codec();
                        if (Defaults.NoCodec.class.equals(codecClass))
                            codecClass = null;
                        break;
                    }
                }
                boolean thisParamNamed = (paramName != null);
                if (allParamsNamed == null)
                    allParamsNamed = thisParamNamed;
                else if (allParamsNamed != thisParamNamed)
                    throw new IllegalArgumentException(String.format("For method '%s', either all or none of the parameters must be named", m.getName()));

                paramMappers[i] = newParamMapper(accClass.getName(), m.getName(), i, paramName, codecClass, paramTypes[i], mappingManager);
            }

            ConsistencyLevel cl = null;
            int fetchSize = -1;
            boolean tracing = false;
            Boolean idempotent = null;

            QueryParameters options = m.getAnnotation(QueryParameters.class);
            if (options != null) {
                cl = options.consistency().isEmpty() ? null : ConsistencyLevel.valueOf(options.consistency().toUpperCase());
                fetchSize = options.fetchSize();
                tracing = options.tracing();
                if (options.idempotent().length > 1) {
                    throw new IllegalArgumentException("idemtpotence() attribute can only accept one value");
                }
                idempotent = options.idempotent().length == 0 ? null : options.idempotent()[0];
            }

            methods.add(new MethodMapper(m, queryString, paramMappers, cl, fetchSize, tracing, idempotent));
        }

        return new AccessorMapper<T>(accClass, methods);
    }

    private static ParamMapper newParamMapper(String className, String methodName, int idx, String paramName, Class<? extends TypeCodec<?>> codecClass, Type paramType, MappingManager mappingManager) {
        if (paramType instanceof Class) {
            Class<?> paramClass = (Class<?>) paramType;
            if (TypeMappings.isMappedUDT(paramClass))
                mappingManager.getUDTCodec(paramClass);

            return new ParamMapper(paramName, idx, TypeToken.of(paramType), codecClass);
        } else if (paramType instanceof ParameterizedType) {
            for (Class<?> udt : TypeMappings.findUDTs(paramType))
                mappingManager.getUDTCodec(udt);

            return new ParamMapper(paramName, idx, TypeToken.of(paramType), codecClass);
        } else {
            throw new IllegalArgumentException(String.format("Cannot map class %s for parameter %s of %s.%s", paramType, paramName, className, methodName));
        }
    }
}
