package com.alvarengacarlos.unnamed.coreapi.tournament;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvarengacarlos.unnamed.coreapi.OrganizationContext;
import com.alvarengacarlos.unnamed.coreapi.common.AuditLog;
import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;
import com.alvarengacarlos.unnamed.coreapi.common.Page;
import com.alvarengacarlos.unnamed.coreapi.common.Pageable;
import com.alvarengacarlos.unnamed.coreapi.member.Member;
import com.alvarengacarlos.unnamed.coreapi.member.MemberInput;
import com.alvarengacarlos.unnamed.coreapi.member.MemberService;
import com.alvarengacarlos.unnamed.coreapi.rank.Rank;
import com.alvarengacarlos.unnamed.coreapi.rank.RankService;

@Service
public class TournamentService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TournamentRepository tournamentRepository;
    private final MemberService memberService;
    private final RankService rankService;

    public TournamentService(
        TournamentRepository tournamentRepository,
        MemberService memberService,
        RankService rankService) {
        this.tournamentRepository = tournamentRepository;
        this.memberService = memberService;
        this.rankService = rankService;
    }

    @Transactional
    public Tournament createTournament(TournamentInput.CreateUpdate input) {
        checkPeriod(input);
        final TournamentDivisionCriterion tournamentDivisionCriterion = mountTournamentDivisionCriterion(
            input.divisionCriterion());
        final Tournament tournament = Tournament.builder()
            .name(input.name())
            .format(input.format())
            .tournamentDivisionCriterion(tournamentDivisionCriterion)
            .participationPrice(input.participationPrice())
            .startDateTime(input.startDateTime())
            .endDateTime(input.endDateTime())
            .where(input.where())
            .organizationId(OrganizationContext.require())
            .build();
        AuditLog.log("creating tournament {}", tournament.getId());
        tournamentRepository.save(tournament);
        return tournament;
    }

    // A tournament may start and end at the same time, but not end before it starts.
    private void checkPeriod(TournamentInput.CreateUpdate input) {
        if (input.endDateTime().isBefore(input.startDateTime())) {
            throw new TournamentEndsBeforeItStartsException();
        }
    }

    private TournamentDivisionCriterion mountTournamentDivisionCriterion(
        TournamentInput.DivisionCriterion divisionCriterion) {
        final Optional<Rank> optionalOfRank = Optional.ofNullable(divisionCriterion.rankId())
            .map(rankService::getRank);

        return new TournamentDivisionCriterion.Builder()
            .gender(Optional.ofNullable(divisionCriterion.gender()))
            .weight(Optional.ofNullable(divisionCriterion.weight()))
            .height(Optional.ofNullable(divisionCriterion.height()))
            .age(Optional.ofNullable(divisionCriterion.age()))
            .isPwd(Optional.ofNullable(divisionCriterion.isPwd()))
            .rank(optionalOfRank)
            .build();
    }

    @Transactional
    public Tournament updateTournament(UUID id, TournamentInput.CreateUpdate input) {
        AuditLog.log("updating tournament {}", id);

        checkPeriod(input);
        final TournamentDivisionCriterion tournamentDivisionCriterion = mountTournamentDivisionCriterion(
            input.divisionCriterion());
        final Tournament tournament = getTournament(id);
        final Tournament updatedTournament = tournament.toBuilder()
            .name(input.name())
            .format(input.format())
            .tournamentDivisionCriterion(tournamentDivisionCriterion)
            .participationPrice(input.participationPrice())
            .startDateTime(input.startDateTime())
            .endDateTime(input.endDateTime())
            .where(input.where())
            .build();
        tournamentRepository.update(updatedTournament);
        return updatedTournament;
    }

    public Tournament getTournament(UUID id) {
        logger.debug("Getting tournament {}", id);
        return tournamentRepository.read(id)
            .orElseThrow(() -> new TournamentNotFoundException());
    }

    public Map<UUID, Tournament> getTournaments(Set<UUID> ids) {
        logger.debug("Getting tournaments {}", ids);

        final Map<UUID, Tournament> tournaments = tournamentRepository.read(ids)
            .stream()
            .collect(Collectors.toMap(Tournament::getId, Function.identity()));
        if (tournaments.size() != ids.size()) {
            throw new TournamentNotFoundException();
        }
        return tournaments;
    }

    @Transactional
    public Tournament deleteTournament(UUID id) {
        AuditLog.log("deleting tournament {}", id);

        final Tournament tournament = getTournament(id);
        tournamentRepository.delete(id);
        return tournament;
    }

    public Page<Tournament> getTournaments(CommonInput.PaginationInput paginationInput) {
        return tournamentRepository.read(Pageable.from(paginationInput));
    }

    public List<Member> getPossibleCompetitors(
        UUID id, 
        TournamentInput.GetPossibleCompetitors input) {

        final Tournament tournament = getTournament(id);
        checkTournamentFormat(tournament);
        
        final TournamentDivisionCriterion divisionCriterion = tournament.getTournamentDivisionCriterion();
        
        final List<Optional<BigDecimal>> minAndMaxWeight = mountMinAndMaxWeightOrHeight(
            divisionCriterion.getWeight(), 
            input.weightDifference());
        final List<Optional<BigDecimal>> minAndMaxHeight = mountMinAndMaxWeightOrHeight(
            divisionCriterion.getHeight(), 
            input.heightDifference());
        final List<Optional<LocalDate>> minAndMaxBirthdate = mountMinAndMaxBirthdate(
            divisionCriterion.getAge(), 
            input.ageDifference());

        Optional<UUID> optionalOfRankId = Optional.empty();
        if (divisionCriterion.getRank().isPresent()) {
            optionalOfRankId = Optional.of(divisionCriterion.getRank()
                .get()
                .getId());
        }
    
        final MemberInput.GetPossibleCompetitors getPossibleCompetitorsInput = new MemberInput.GetPossibleCompetitors(
            divisionCriterion.getGender(), 
            minAndMaxWeight.get(0),
            minAndMaxWeight.get(1),
            minAndMaxHeight.get(0),
            minAndMaxHeight.get(1),
            minAndMaxBirthdate.get(0),
            minAndMaxBirthdate.get(1),
            divisionCriterion.getIsPwd(),
            optionalOfRankId
            );
        return memberService.getPossibleCompetitors(getPossibleCompetitorsInput);
    }

    private void checkTournamentFormat(Tournament tournament) {
        if (Tournament.TournamentFormat.TITLE_MATCH.equals(tournament.getFormat())) {
            throw new TournamentFormatIsNotSupportedForThisOperationException(tournament.getFormat());
        }
    }

    private List<Optional<BigDecimal>> mountMinAndMaxWeightOrHeight(
        Optional<BigDecimal> optionalOfWeightOrHeight, 
        BigDecimal weightOrHeightDifference) {
        logger.debug("Mounting min and max weight");
        Optional<BigDecimal> minWeight = Optional.empty();
        Optional<BigDecimal> maxWeight = Optional.empty();
        if (optionalOfWeightOrHeight.isPresent() && Objects.nonNull(weightOrHeightDifference)) {
            minWeight = Optional.of(optionalOfWeightOrHeight.get().subtract(weightOrHeightDifference));
            maxWeight = Optional.of(optionalOfWeightOrHeight.get().add(weightOrHeightDifference));
        }

        return List.of(minWeight, maxWeight);
    }

    private List<Optional<LocalDate>> mountMinAndMaxBirthdate(Optional<Byte> optionalOfAge, Byte ageDifference) {
        logger.debug("Mounting min and max birthdate");
        Optional<LocalDate> minBirthdate = Optional.empty();
        Optional<LocalDate> maxBirthdate = Optional.empty();
        if (optionalOfAge.isPresent() && Objects.nonNull(ageDifference)) {
            final LocalDate now = LocalDate.now();
            final LocalDate localDate = now.minusYears(optionalOfAge.get());
            minBirthdate = Optional.of(localDate.minusYears(ageDifference));
            maxBirthdate = Optional.of(localDate.plusYears(ageDifference));
        }

        return List.of(minBirthdate, maxBirthdate);
    }
}
