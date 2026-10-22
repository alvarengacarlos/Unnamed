package com.alvarengacarlos.unnamed.app.tournamentcompetitor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvarengacarlos.unnamed.app.OrganizationContext;
import com.alvarengacarlos.unnamed.app.common.AuditLog;
import com.alvarengacarlos.unnamed.app.common.BulkHasDuplicatedItemException;
import com.alvarengacarlos.unnamed.app.common.CommonInput;
import com.alvarengacarlos.unnamed.app.common.Page;
import com.alvarengacarlos.unnamed.app.common.Pageable;
import com.alvarengacarlos.unnamed.app.member.Member;
import com.alvarengacarlos.unnamed.app.member.MemberService;
import com.alvarengacarlos.unnamed.app.tournament.Tournament;
import com.alvarengacarlos.unnamed.app.tournament.TournamentService;

@Service
public class TournamentCompetitorService {
    private final String creatingLog = "creating tournament competitor {}";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TournamentCompetitorRepository tournamentCompetitorRepository;
    private final TournamentService tournamentService;
    private final MemberService memberService;

    public TournamentCompetitorService(
        TournamentCompetitorRepository tournamentCompetitorRepository,
        TournamentService tournamentService,
        MemberService memberService) {
        this.tournamentCompetitorRepository = tournamentCompetitorRepository;
        this.tournamentService = tournamentService;
        this.memberService = memberService;
    }

    @Transactional
    public TournamentCompetitor createTournamentCompetitor(
        TournamentCompetitorInput.Create input) {
        final TournamentCompetitor tournamentCompetitor = mountTournamentCompetitor(
            input,
            tournamentService.getTournament(input.tournamentId()),
            memberService.getMember(input.memberId()));
        checkIfTournamentCompetitorExists(input.tournamentId(), input.memberId());

        AuditLog.log(creatingLog, tournamentCompetitor.getId());
        tournamentCompetitorRepository.save(tournamentCompetitor);
        return tournamentCompetitor;
    }

    private TournamentCompetitor mountTournamentCompetitor(
        TournamentCompetitorInput.Create input,
        Tournament tournament,
        Member member) {
        return TournamentCompetitor.builder()
            .paymentMethod(input.paymentMethod())
            .isPaid(input.isPaid())
            .points((short) 0)
            .tournament(tournament)
            .member(member)
            .organizationId(OrganizationContext.require())
            .build();
    }

    private void checkIfTournamentCompetitorExists(UUID tournamentId, UUID memberId) {
        if (tournamentCompetitorRepository.exists(tournamentId, memberId)) {
            throw new TournamentCompetitorExistsException();
        }
    }

    @Transactional
    public TournamentCompetitor updateTournamentCompetitor(
        UUID id, 
        TournamentCompetitorInput.Update input) {
        AuditLog.log("updating tournament competitor {}", id);

        final TournamentCompetitor tournamentCompetitor = getTournamentCompetitor(id);
        
        final TournamentCompetitor updatedTournamentCompetitor = tournamentCompetitor.toBuilder()
            .paymentMethod(input.paymentMethod())
            .isPaid(input.isPaid())
            .build();

        tournamentCompetitorRepository.update(updatedTournamentCompetitor);
        return updatedTournamentCompetitor;
    }

    public TournamentCompetitor getTournamentCompetitor(UUID id) {
        logger.debug("Getting tournament competitor {}", id);

        return tournamentCompetitorRepository.read(id)
            .orElseThrow(() -> new TournamentCompetitorNotFoundException());
    }

    public Map<UUID, TournamentCompetitor> getTournamentCompetitors(Set<UUID> ids) {
        logger.debug("Getting tournament competitors {}", ids);

        final Map<UUID, TournamentCompetitor> tournamentCompetitors = tournamentCompetitorRepository.read(ids)
            .stream()
            .collect(Collectors.toMap(TournamentCompetitor::getId, Function.identity()));
        if (tournamentCompetitors.size() != ids.size()) {
            throw new TournamentCompetitorNotFoundException();
        }
        return tournamentCompetitors;
    }

    @Transactional
    public TournamentCompetitor deleteTournamentCompetitor(UUID id) {
        AuditLog.log("deleting tournament competitor {}", id);

        final TournamentCompetitor tournamentCompetitor = getTournamentCompetitor(id);
        tournamentCompetitorRepository.delete(id);
        return tournamentCompetitor;
    }

    public Page<TournamentCompetitor> getTournamentCompetitors(
        CommonInput.PaginationInput paginationInput,
        TournamentCompetitorInput.GetTournamentCompetitors filterInput) {
        return tournamentCompetitorRepository.read(Pageable.from(paginationInput), filterInput);
    }

    @Transactional
    public List<TournamentCompetitor> createTournamentCompetitorsInBulk(
        TournamentCompetitorInput.CreateInBulk input) {
        checkIfHasDuplicatedItem(input);

        // One query for each kind of data, whatever the size of the bulk.
        final Map<UUID, Tournament> tournaments = tournamentService.getTournaments(input.bulk()
            .stream()
            .map(TournamentCompetitorInput.Create::tournamentId)
            .collect(Collectors.toSet()));
        final Map<UUID, Member> members = memberService.getMembers(input.bulk()
            .stream()
            .map(TournamentCompetitorInput.Create::memberId)
            .collect(Collectors.toSet()));

        final List<TournamentCompetitor> tournamentCompetitors = input.bulk()
            .stream()
            .map(tc -> mountTournamentCompetitor(tc, tournaments.get(tc.tournamentId()), members.get(tc.memberId())))
            .toList();
        if (tournamentCompetitorRepository.exists(tournamentCompetitors)) {
            throw new TournamentCompetitorExistsException();
        }

        tournamentCompetitors.forEach(created -> AuditLog.log(creatingLog, created.getId()));
        tournamentCompetitorRepository.saveInBulk(tournamentCompetitors);
        
        return tournamentCompetitors;
    }

    private void checkIfHasDuplicatedItem(TournamentCompetitorInput.CreateInBulk input) {
        final Boolean hasDuplicatedItem = input.bulk()
            .stream()
            .map(b -> List.of(b.tournamentId(), b.memberId()))
            .distinct()
            .count() != input.bulk().size();
        if (hasDuplicatedItem) {
            throw new BulkHasDuplicatedItemException();
        }
    }
}
