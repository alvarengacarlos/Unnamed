package com.alvarengacarlos.unnamed.app.member;

import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertSameEntity;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import com.alvarengacarlos.unnamed.app.common.Page;
import com.alvarengacarlos.unnamed.app.common.Pageable;
import com.alvarengacarlos.unnamed.app.rank.Rank;
import com.alvarengacarlos.unnamed.app.testsupport.Fixtures;
import com.alvarengacarlos.unnamed.app.testsupport.IntegrationTest;
import com.alvarengacarlos.unnamed.app.testsupport.OrganizationScope;
import com.alvarengacarlos.unnamed.app.testsupport.OrganizationScoped;

@IntegrationTest
public class MemberRepositoryIT implements OrganizationScoped {
    private static final BigDecimal WEIGHT = new BigDecimal("80.50");
    private static final BigDecimal HEIGHT = new BigDecimal("1.80");
    private static final LocalDate BIRTH_DATE = LocalDate.of(2000, 1, 1);
    private static final String UPDATED_NAME = "Updated name";
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private MemberRepository memberRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @Test
    void shouldSaveAMember() {
        final Member member = fixtures.newMember(fixtures.createRank());

        memberRepository.save(member);

        assertSameEntity(memberRepository.read(member.getId()).orElseThrow(), member);
    }

    @Test
    void shouldUpdateAMember() {
        final Member member = fixtures.createMember();
        final Member updatedMember = member.toBuilder()
            .name(UPDATED_NAME)
            .phone(Fixtures.randomPhone())
            .cpf(Fixtures.randomCpf())
            .address("Updated address")
            .gender(member.getGender() == Member.Gender.MALE ? Member.Gender.FEMALE : Member.Gender.MALE)
            .weight(member.getWeight().add(BigDecimal.ONE))
            .height(member.getHeight().add(BigDecimal.ONE))
            .birthDate(member.getBirthDate().minusDays(1))
            .isPwd(!member.getIsPwd())
            .isCompetitor(!member.getIsCompetitor())
            .rank(fixtures.createRank())
            .build();

        memberRepository.update(updatedMember);

        assertSameEntity(memberRepository.read(member.getId()).orElseThrow(), updatedMember);
    }

    @Test
    void shouldNotReadAMemberThatDoesNotExist() {
        assertThat(memberRepository.read(UUID.randomUUID())).isEmpty();
    }

    @Nested
    class ExistsByCpf {
        @Test
        void shouldFindAMemberWithTheCpf() {
            final Member member = fixtures.createMember();

            assertThat(memberRepository.existsByCpf(member.getCpf(), Optional.empty())).isTrue();
        }

        @Test
        void shouldNotFindAMemberWithAnotherCpf() {
            fixtures.createMember();

            assertThat(memberRepository.existsByCpf(Fixtures.randomCpf(), Optional.empty())).isFalse();
        }

        @Test
        void shouldIgnoreTheExcludedMember() {
            final Member member = fixtures.createMember();

            assertThat(memberRepository.existsByCpf(member.getCpf(), Optional.of(member.getId()))).isFalse();
        }
    }

    @Nested
    class ReadMany {
        @Test
        void shouldReadNone() {
            final Page<Member> page = memberRepository.read(
                new Pageable(0, 1),
                new MemberInput.GetMembers(Optional.empty(), Optional.empty()));

            assertThat(page.content()).isEmpty();
            assertThat(page.totalElements()).isZero();
        }

        @Test
        void shouldReadAll() {
            final List<Member> members = List.of(
                fixtures.createMember(),
                fixtures.createMember(),
                fixtures.createMember());

            final Page<Member> page = memberRepository.read(
                new Pageable(0, 10),
                new MemberInput.GetMembers(Optional.empty(), Optional.empty()));

            assertSameEntity(page.content(), members);
            assertThat(page.totalElements()).isEqualTo(3);
        }

        @Test
        void shouldReadTheLastPage() {
            fixtures.createMember();
            fixtures.createMember();
            fixtures.createMember();

            final Page<Member> page = memberRepository.read(
                new Pageable(1, 2),
                new MemberInput.GetMembers(Optional.empty(), Optional.empty()));

            assertThat(page.content()).hasSize(1);
            assertThat(page.totalElements()).isEqualTo(3);
        }

        @Test
        void shouldReadOnlyTheMemberWithTheCpf() {
            final Member member = fixtures.createMember();
            fixtures.createMember();

            final Page<Member> page = memberRepository.read(
                new Pageable(0, 10),
                new MemberInput.GetMembers(Optional.empty(), Optional.of(member.getCpf())));

            assertSameEntity(page.content(), List.of(member));
            assertThat(page.totalElements()).isEqualTo(1);
        }

        @Test
        void shouldReadOnlyTheMembersWithTheName() {
            final Member member = fixtures.createMember(fixtures.createRank(), builder -> builder.name("João Carlos"));
            fixtures.createMember(fixtures.createRank(), builder -> builder.name("Pedro Souza"));

            final Page<Member> page = memberRepository.read(
                new Pageable(0, 10),
                new MemberInput.GetMembers(Optional.of("carlos"), Optional.empty()));

            assertSameEntity(page.content(), List.of(member));
            assertThat(page.totalElements()).isEqualTo(1);
        }

        @Test
        void shouldReadTheMostRecentFirst() {
            final Instant now = Instant.now();
            final Member oldest = fixtures.createMember(
                fixtures.createRank(),
                builder -> builder.createdAt(now.minus(2, ChronoUnit.DAYS)));
            final Member newest = fixtures.createMember(fixtures.createRank(), builder -> builder.createdAt(now));
            final Member middle = fixtures.createMember(
                fixtures.createRank(),
                builder -> builder.createdAt(now.minus(1, ChronoUnit.DAYS)));

            final Page<Member> page = memberRepository.read(
                new Pageable(0, 10),
                new MemberInput.GetMembers(Optional.empty(), Optional.empty()));

            assertThat(page.content())
                .extracting(Member::getId)
                .containsExactly(newest.getId(), middle.getId(), oldest.getId());
        }
    }

    @Nested
    class ReadPossibleCompetitors {
        private Rank rank;
        private Member competitor;

        static Stream<Arguments> matchingFilters() {
            return Stream.of(
                Arguments.of("no filter", filter(_ -> new Filter())),
                Arguments.of("same gender", filter(_ -> new Filter().gender(Member.Gender.MALE))),
                Arguments.of("weight on the range limits", filter(_ -> new Filter().weight(WEIGHT, WEIGHT))),
                Arguments.of("height on the range limits", filter(_ -> new Filter().height(HEIGHT, HEIGHT))),
                Arguments.of(
                    "birth date on the range limits",
                    filter(_ -> new Filter().birthDate(BIRTH_DATE, BIRTH_DATE))),
                Arguments.of("same PwD status", filter(_ -> new Filter().isPwd(false))),
                Arguments.of("same rank", filter(rankId -> new Filter().rankId(rankId))));
        }

        static Stream<Arguments> notMatchingFilters() {
            final BigDecimal cent = new BigDecimal("0.01");
            return Stream.of(
                Arguments.of("other gender", filter(_ -> new Filter().gender(Member.Gender.FEMALE))),
                Arguments.of(
                    "weight below the range",
                    filter(_ -> new Filter().weight(WEIGHT.add(cent), WEIGHT.add(BigDecimal.TEN)))),
                Arguments.of(
                    "height below the range",
                    filter(_ -> new Filter().height(HEIGHT.add(cent), HEIGHT.add(BigDecimal.ONE)))),
                Arguments.of(
                    "birth date before the range",
                    filter(_ -> new Filter().birthDate(BIRTH_DATE.plusDays(1), BIRTH_DATE.plusYears(1)))),
                Arguments.of("other PwD status", filter(_ -> new Filter().isPwd(true))),
                Arguments.of("other rank", filter(_ -> new Filter().rankId(UUID.randomUUID()))));
        }

        // Keeps the generic type of the lambda when it goes through Arguments.of(Object...).
        private static Function<UUID, Filter> filter(Function<UUID, Filter> filter) {
            return filter;
        }

        @BeforeEach
        void beforeEach() {
            rank = fixtures.createRank();
            competitor = fixtures.createMember(rank, member -> member
                .gender(Member.Gender.MALE)
                .weight(WEIGHT)
                .height(HEIGHT)
                .birthDate(BIRTH_DATE)
                .isPwd(false)
                .isCompetitor(true));
            fixtures.createMember(rank, member -> member
                .gender(Member.Gender.MALE)
                .weight(WEIGHT)
                .height(HEIGHT)
                .birthDate(BIRTH_DATE)
                .isPwd(false)
                .isCompetitor(false));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("matchingFilters")
        void shouldReadOnlyTheCompetitorWhenItMatches(String filterName, Function<UUID, Filter> filter) {
            final List<Member> members = memberRepository.read(filter.apply(rank.getId()).build());

            assertSameEntity(members, List.of(competitor));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("notMatchingFilters")
        void shouldReadNoneWhenTheCompetitorDoesNotMatch(String filterName, Function<UUID, Filter> filter) {
            assertThat(memberRepository.read(filter.apply(rank.getId()).build())).isEmpty();
        }
    }

    @Test
    void shouldDeleteAMember() {
        final Member member = fixtures.createMember();

        memberRepository.delete(member.getId());

        assertThat(memberRepository.read(member.getId())).isEmpty();
    }

    @Nested
    class WhenTheMemberBelongsToAnotherOrganization {
        private Member member;

        @BeforeEach
        void beforeEach() {
            member = OrganizationScope.call(
                anotherOrganizationId,
                () -> fixtures.createMember(fixtures.createRank(), builder -> builder.isCompetitor(true)));
        }

        @Test
        void shouldNotReadIt() {
            assertThat(memberRepository.read(member.getId())).isEmpty();
            assertThat(memberRepository.existsByCpf(member.getCpf(), Optional.empty())).isFalse();
            assertThat(memberRepository.read(
                    new Pageable(0, 1),
                    new MemberInput.GetMembers(Optional.empty(), Optional.empty()))
                .totalElements()).isZero();
            assertThat(memberRepository.read(new Filter().build())).isEmpty();
        }

        @Test
        void shouldNotUpdateIt() {
            memberRepository.update(member.toBuilder().name(UPDATED_NAME).build());

            assertSameEntity(readInAnotherOrganization(), member);
        }

        @Test
        void shouldNotDeleteIt() {
            memberRepository.delete(member.getId());

            assertSameEntity(readInAnotherOrganization(), member);
        }

        private Member readInAnotherOrganization() {
            return OrganizationScope.call(
                anotherOrganizationId,
                () -> memberRepository.read(member.getId()).orElseThrow());
        }
    }

    private static class Filter {
        private Optional<Member.Gender> gender = Optional.empty();
        private Optional<BigDecimal> minWeight = Optional.empty();
        private Optional<BigDecimal> maxWeight = Optional.empty();
        private Optional<BigDecimal> minHeight = Optional.empty();
        private Optional<BigDecimal> maxHeight = Optional.empty();
        private Optional<LocalDate> minBirthDate = Optional.empty();
        private Optional<LocalDate> maxBirthDate = Optional.empty();
        private Optional<Boolean> isPwd = Optional.empty();
        private Optional<UUID> rankId = Optional.empty();

        Filter gender(Member.Gender gender) {
            this.gender = Optional.of(gender);
            return this;
        }

        Filter weight(BigDecimal min, BigDecimal max) {
            minWeight = Optional.of(min);
            maxWeight = Optional.of(max);
            return this;
        }

        Filter height(BigDecimal min, BigDecimal max) {
            minHeight = Optional.of(min);
            maxHeight = Optional.of(max);
            return this;
        }

        Filter birthDate(LocalDate min, LocalDate max) {
            minBirthDate = Optional.of(min);
            maxBirthDate = Optional.of(max);
            return this;
        }

        Filter isPwd(Boolean isPwd) {
            this.isPwd = Optional.of(isPwd);
            return this;
        }

        Filter rankId(UUID rankId) {
            this.rankId = Optional.of(rankId);
            return this;
        }

        MemberInput.GetPossibleCompetitors build() {
            return new MemberInput.GetPossibleCompetitors(
                gender,
                minWeight,
                maxWeight,
                minHeight,
                maxHeight,
                minBirthDate,
                maxBirthDate,
                isPwd,
                rankId);
        }
    }
}
