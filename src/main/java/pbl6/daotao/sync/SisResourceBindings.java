package pbl6.daotao.sync;

import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
/** P02 bindings only. Staff/CVHT remain gated in P15; units retains its existing engine. */
@Configuration(proxyBeanMethods=false)
@ConditionalOnProperty(name="ct26.enabled",havingValue="true")
public class SisResourceBindings {
    @Bean CtBinding ethnicitiesBinding(){return SisResourceMapper.binding("ethnicities");}
    @Bean CtBinding nationalitiesBinding(){return SisResourceMapper.binding("nationalities");}
    @Bean CtBinding localitiesBinding(){return SisResourceMapper.binding("localities");}
    @Bean CtBinding plansBinding(){return SisResourceMapper.binding("plans");}
    @Bean CtBinding cohortsBinding(){return SisResourceMapper.binding("cohorts");}
    @Bean CtBinding nationalMajorsBinding(){return SisResourceMapper.binding("national-majors");}
    @Bean CtBinding internalMajorsBinding(){return SisResourceMapper.binding("internal-majors");}
    @Bean CtBinding programsBinding(){return SisResourceMapper.binding("programs");}
    @Bean CtBinding classesBinding(){return SisResourceMapper.binding("classes");}
    @Bean CtBinding studentsBinding(){return SisResourceMapper.binding("students");}
    @Bean CtBinding academicResultsBinding(){return SisResourceMapper.binding("academic-results");}
    @Bean CtBinding coursesBinding(){return SisResourceMapper.binding("courses");}
    @Bean CtBinding curriculumItemsBinding(){return SisResourceMapper.binding("curriculum-items");}
    @Bean CtBinding courseResultsBinding(){return SisResourceMapper.binding("course-results");}
    @Bean CtBinding recognizedCourseResultsBinding(){return SisResourceMapper.binding("recognized-course-results");}
    @Bean CtBinding referenceValuesBinding(){return SisResourceMapper.binding("reference-values");}
    @Bean CtBinding roomsBinding(){return SisResourceMapper.binding("rooms");}
    @Bean CtBinding roomUsageBinding(){return SisResourceMapper.binding("room-usage");}
    @Bean CtBinding roomLocksBinding(){return SisResourceMapper.binding("room-locks");}
    @Bean CtBinding roomLockDetailsBinding(){return SisResourceMapper.binding("room-lock-details");}
    @Bean CtBinding conductClassAssignmentsBinding(){return SisResourceMapper.binding("conduct-class-assignments");}
    @Bean CtBinding classMergesBinding(){return SisResourceMapper.binding("class-merges");}
    @Bean CtBinding studentProgressBinding(){return SisResourceMapper.binding("student-progress");}
}
