package de.tonypsilon.rankify.api.poll.business;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CastVoteUseCaseTest {

    @Mock
    private PollRepository pollRepository;

    @Mock
    private VoteFactory voteFactory;

    private CastVoteUseCase castVoteUseCase;

    private PollId pollId;
    private Poll poll;
    private Option optionA;
    private Option optionB;

    @BeforeEach
    void setUp() {
        castVoteUseCase = new CastVoteUseCase(pollRepository, voteFactory);
        pollId = new PollId(UUID.randomUUID());
        optionA = new Option("Option A");
        optionB = new Option("Option B");
        Ballot ballot = new Ballot(List.of(optionA, optionB));
        Schedule schedule = new Schedule(LocalDateTime.now().minusSeconds(10), null);
        poll = new Poll(pollId, new PollTitle("Test Poll"), ballot, schedule, LocalDateTime.now());
    }

    @Test
    void castVote_withValidInput_shouldRetrievePollCreateVoteAndSave() {
        // Given
        Map<Option, Integer> rankings = Map.of(optionA, 1);
        Vote expectedVote = new RecordedVote(pollId, Map.of(
                optionA, 1,
                optionB, RecordedVote.MAX_RANKING
        ));

        when(pollRepository.getById(pollId)).thenReturn(poll);
        when(voteFactory.createVote(poll, rankings)).thenReturn(expectedVote);

        // When
        castVoteUseCase.castVote(pollId, rankings);

        // Then
        verify(pollRepository).getById(pollId);
        verify(voteFactory).createVote(poll, rankings);
        verify(pollRepository).saveVote(expectedVote);
    }

    @Test
    void castVote_withNullPollId_shouldThrowIllegalArgumentException() {
        // Given
        Map<Option, Integer> rankings = Map.of(optionA, 1);

        // When & Then
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> castVoteUseCase.castVote(null, rankings))
                .withMessage("pollId must not be null");
    }

    @Test
    void castVote_withNullRankings_shouldThrowNullPointerException() {
        // When & Then
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> castVoteUseCase.castVote(pollId, null))
                .withMessageContaining("rankings must not be null");
    }

    @Test
    void castVote_whenPollNotFound_shouldPropagateException() {
        // Given
        Map<Option, Integer> rankings = Map.of(optionA, 1);
        when(pollRepository.getById(pollId)).thenThrow(new PollNotFoundException(pollId));

        // When & Then
        assertThatExceptionOfType(PollNotFoundException.class)
                .isThrownBy(() -> castVoteUseCase.castVote(pollId, rankings));
    }

    @Test
    void castVote_whenPollNotReady_shouldPropagateException() {
        // Given
        Map<Option, Integer> rankings = Map.of(optionA, 1);
        when(pollRepository.getById(pollId)).thenReturn(poll);
        when(voteFactory.createVote(poll, rankings))
                .thenThrow(new PollNotReadyForVotingException(pollId));

        // When & Then
        assertThatExceptionOfType(PollNotReadyForVotingException.class)
                .isThrownBy(() -> castVoteUseCase.castVote(pollId, rankings));
    }

    @Test
    void castVote_shouldDelegateValidationToVoteFactory() {
        // Given
        Map<Option, Integer> rankings = Map.of(optionA, 1);
        Vote expectedVote = new RecordedVote(pollId, Map.of(
                optionA, 1,
                optionB, RecordedVote.MAX_RANKING
        ));

        when(pollRepository.getById(pollId)).thenReturn(poll);
        when(voteFactory.createVote(poll, rankings)).thenReturn(expectedVote);

        // When
        castVoteUseCase.castVote(pollId, rankings);

        // Then
        // Verify that the factory is called with the correct poll and rankings
        ArgumentCaptor<Poll> pollCaptor = ArgumentCaptor.forClass(Poll.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Option, Integer>> rankingsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(voteFactory).createVote(pollCaptor.capture(), rankingsCaptor.capture());

        assertThat(pollCaptor.getValue()).isEqualTo(poll);
        assertThat(rankingsCaptor.getValue()).isEqualTo(rankings);
    }

    @Test
    void castVote_shouldSaveReturnedVoteFromFactory() {
        // Given
        Map<Option, Integer> rankings = Map.of(optionA, 1);
        Vote expectedVote = new RecordedVote(pollId, Map.of(
                optionA, 1,
                optionB, RecordedVote.MAX_RANKING
        ));

        when(pollRepository.getById(pollId)).thenReturn(poll);
        when(voteFactory.createVote(poll, rankings)).thenReturn(expectedVote);

        // When
        castVoteUseCase.castVote(pollId, rankings);

        // Then
        ArgumentCaptor<Vote> voteCaptor = ArgumentCaptor.forClass(Vote.class);
        verify(pollRepository).saveVote(voteCaptor.capture());
        assertThat(voteCaptor.getValue()).isEqualTo(expectedVote);
    }
}
