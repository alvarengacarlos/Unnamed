package com.alvarengacarlos.unnamed.coreapi.clazz;

import static com.alvarengacarlos.unnamed.coreapi.common.Joins.nested;
import static com.alvarengacarlos.unnamed.coreapi.jooq.Tables.CLAZZES;
import static org.jooq.impl.DSL.noCondition;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.springframework.stereotype.Repository;

import com.alvarengacarlos.unnamed.coreapi.OrganizationContext;
import com.alvarengacarlos.unnamed.coreapi.common.Joins;
import com.alvarengacarlos.unnamed.coreapi.common.Page;
import com.alvarengacarlos.unnamed.coreapi.common.Pageable;
import com.alvarengacarlos.unnamed.coreapi.common.Pagination;
import com.alvarengacarlos.unnamed.coreapi.jooq.tables.Clazzes;
import com.alvarengacarlos.unnamed.coreapi.jooq.tables.records.ClazzesRecord;
import com.alvarengacarlos.unnamed.coreapi.teacher.TeacherRepository;

@Repository
public class ClazzRepository {
    private static final String ALIAS = "cl";
    private static final String TEACHER = "t";
    private final DSLContext dsl;

    public ClazzRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void save(Clazz clazz) {
        dsl.insertInto(CLAZZES)
            .set(CLAZZES.ID, clazz.getId())
            .set(CLAZZES.NAME, clazz.getName())
            .set(CLAZZES.MARTIAL_ART, clazz.getMartialArt())
            .set(CLAZZES.START_TIME, clazz.getStartTime())
            .set(CLAZZES.END_TIME, clazz.getEndTime())
            .set(CLAZZES.WEEKDAYS, toArray(clazz.getWeekdays()))
            .set(CLAZZES.CREATED_AT, clazz.getCreatedAt())
            .set(CLAZZES.UPDATED_AT, clazz.getUpdatedAt())
            .set(CLAZZES.TEACHER_ID, clazz.getTeacher().getId())
            .set(CLAZZES.ORGANIZATION_ID, OrganizationContext.require())
            .execute();
    }

    public void update(Clazz clazz) {
        dsl.update(CLAZZES)
            .set(CLAZZES.NAME, clazz.getName())
            .set(CLAZZES.MARTIAL_ART, clazz.getMartialArt())
            .set(CLAZZES.START_TIME, clazz.getStartTime())
            .set(CLAZZES.END_TIME, clazz.getEndTime())
            .set(CLAZZES.WEEKDAYS, toArray(clazz.getWeekdays()))
            .set(CLAZZES.UPDATED_AT, clazz.getUpdatedAt())
            .set(CLAZZES.TEACHER_ID, clazz.getTeacher().getId())
            .where(CLAZZES.ID.eq(clazz.getId()))
            .and(CLAZZES.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    private String[] toArray(Set<DayOfWeek> weekdays) {
        return weekdays.stream()
            .map(DayOfWeek::name)
            .toArray(String[]::new);
    }

    public Optional<Clazz> read(UUID id) {
        final Clazzes clazz = CLAZZES.as(ALIAS);
        return select()
            .where(clazz.ID.eq(id))
            .and(clazz.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .fetchOptional(record -> toClazz(record, ALIAS));
    }

    public Page<Clazz> read(Pageable pageable, ClazzInput.GetClazzes filterInput) {
        final Clazzes clazz = CLAZZES.as(ALIAS);
        return Pagination.page(
            select().where(
                clazz.ORGANIZATION_ID.eq(OrganizationContext.require()),
                filterInput.martialArt().map(clazz.MARTIAL_ART::eq).orElse(noCondition())),
            pageable,
            Pagination.newestFirst(clazz.CREATED_AT, clazz.ID),
            record -> toClazz(record, ALIAS));
    }

    public void delete(UUID id) {
        dsl.deleteFrom(CLAZZES)
            .where(CLAZZES.ID.eq(id))
            .and(CLAZZES.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    private SelectJoinStep<Record> select() {
        return joinRelations(dsl.select(fields(ALIAS)).from(CLAZZES.as(ALIAS)), ALIAS);
    }

    public static List<Field<?>> fields(String alias) {
        return Joins.concat(
            Arrays.asList(CLAZZES.as(alias).fields()),
            TeacherRepository.fields(nested(alias, TEACHER)));
    }

    public static SelectJoinStep<Record> join(
        SelectJoinStep<Record> select,
        String alias,
        Field<UUID> foreignKey) {
        final Clazzes clazz = CLAZZES.as(alias);
        return joinRelations(select.join(clazz).on(clazz.ID.eq(foreignKey)), alias);
    }

    private static SelectJoinStep<Record> joinRelations(SelectJoinStep<Record> select, String alias) {
        return TeacherRepository.join(select, nested(alias, TEACHER), CLAZZES.as(alias).TEACHER_ID);
    }

    public static Clazz toClazz(Record record, String alias) {
        final ClazzesRecord clazz = record.into(CLAZZES.as(alias));
        final Set<DayOfWeek> weekdays = Arrays.stream(clazz.getWeekdays())
            .map(DayOfWeek::valueOf)
            .collect(Collectors.toSet());
        return Clazz.builder()
            .id(clazz.getId())
            .name(clazz.getName())
            .martialArt(clazz.getMartialArt())
            .startTime(clazz.getStartTime())
            .endTime(clazz.getEndTime())
            .weekdays(weekdays)
            .createdAt(clazz.getCreatedAt())
            .updatedAt(clazz.getUpdatedAt())
            .teacher(TeacherRepository.toTeacher(record, nested(alias, TEACHER)))
            .organizationId(clazz.getOrganizationId())
            .build();
    }
}
