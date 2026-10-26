import { RequestClient } from './common/common.service';
import { RankService, RankCategoryService } from './rank/rank.service';
import { MemberService, StudentService, TeacherService } from './member/member.service';
import { TuitionService } from './tuition/tuition.service';
import { ClazzService } from './clazz/clazz.service';
import { StudentEnrollmentService } from './student-enrollment/student-enrollment.service';
import { TuitionPaymentService } from './tuition-payment/tuition-payment.service';
import { GradingService } from './grading/grading.service';
import { GradingCandidateService } from './grading-candidate/grading-candidate.service';
import { TournamentService } from './tournament/tournament.service';
import { TournamentCompetitorService } from './tournament-competitor/tournament-competitor.service';
import { TournamentMatchService } from './tournament-match/tournament-match.service';
import { SessionService } from './session/session.service';
import { AssistantService } from './assistant/assistant.service';

export const sessionService = new SessionService(
    import.meta.env.VITE_OIDC_AUTHORITY,
    import.meta.env.VITE_OIDC_CLIENT_ID
)

export const requestClient = new RequestClient(
    import.meta.env.VITE_API_BASE_URL,
    () => sessionService.getAccessToken()
)

export const rankCategoryService = new RankCategoryService(requestClient)
export const rankService = new RankService(requestClient)

export const memberService = new MemberService(requestClient)
export const studentService = new StudentService(requestClient)
export const teacherService = new TeacherService(requestClient)

export const tuitionService = new TuitionService(requestClient)
export const tuitionPaymentService = new TuitionPaymentService(requestClient)

export const clazzService = new ClazzService(requestClient)

export const studentEnrollmentService = new StudentEnrollmentService(requestClient)

export const gradingService = new GradingService(requestClient)
export const gradingCandidateService = new GradingCandidateService(requestClient)

export const tournamentService = new TournamentService(requestClient)
export const tournamentCompetitorService = new TournamentCompetitorService(requestClient)
export const tournamentMatchService = new TournamentMatchService(requestClient)

export const assistantService = new AssistantService(requestClient)
