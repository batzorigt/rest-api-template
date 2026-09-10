package rest.api.genre;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.ebean.DB;
import io.javalin.http.BadRequestResponse;
import rest.api.HandlerTransactionTestSupport;
import rest.api.PagedData;
import rest.api.genre.query.QDGenre;

class GenreHandlerTransactionTest extends HandlerTransactionTestSupport {

    private final String genreKey = "transaction-" + UUID.randomUUID();

    @AfterEach
    void removeFixtures() {
        try {
            checkTransactionCleanup();
        } finally {
            new QDGenre().key.equalTo(genreKey).delete();
        }
    }

    @Test
    void getGenresUsesReadOnlyTransactionAndClosesIt() {
        assertReadOnlyTransactionDeclared(GenreHandler.class, "getGenres");
        DGenre existing = insertGenre();
        doAnswer(invocation -> {
            captureTransaction();
            PagedData<Genre> result = invocation.getArgument(0);
            assertTrue(result.getData().stream().anyMatch(genre -> existing.getId().equals(genre.getId())));
            return ctx;
        }).when(ctx).json(any());

        GenreHandler.getGenres(ctx);

        assertTransactionClosed();
        verify(ctx).status(200);
        assertNotNull(DB.find(DGenre.class, existing.getId()));
    }

    @Test
    void getGenresClosesReadOnlyTransactionWhenResponseFails() {
        assertReadOnlyTransactionDeclared(GenreHandler.class, "getGenres");
        DGenre existing = insertGenre();
        IllegalStateException failure = new IllegalStateException("Cannot render genres");
        doAnswer(invocation -> {
            captureTransaction();
            assertNotNull(DB.find(DGenre.class, existing.getId()));
            throw failure;
        }).when(ctx).json(any());

        assertSame(failure, assertThrows(IllegalStateException.class, () -> GenreHandler.getGenres(ctx)));

        assertTransactionClosed();
        assertNotNull(DB.find(DGenre.class, existing.getId()));
    }

    @Test
    void addGenreCommitsAfterTheHandlerReturns() {
        givenValidInput();
        doAnswer(invocation -> {
            captureTransaction(false);
            Genre result = invocation.getArgument(0);
            assertEquals(genreKey, DB.find(DGenre.class, result.getId()).getKey());
            return ctx;
        }).when(ctx).json(any());

        GenreHandler.addGenre(ctx);

        assertTransactionClosed();
        verify(ctx).status(201);
        DGenre committed = new QDGenre().key.equalTo(genreKey).findOne();
        assertNotNull(committed);
        assertEquals("TxGenre", committed.getName());
        assertEquals(3, committed.getOrderNumber());
    }

    @Test
    void addGenreRollsBackSavedGenreWhenResponseFails() {
        givenValidInput();
        IllegalStateException failure = new IllegalStateException("Cannot render added genre");
        doAnswer(invocation -> {
            captureTransaction(false);
            Genre result = invocation.getArgument(0);
            assertEquals(genreKey, DB.find(DGenre.class, result.getId()).getKey());
            throw failure;
        }).when(ctx).json(any());

        assertSame(failure, assertThrows(IllegalStateException.class, () -> GenreHandler.addGenre(ctx)));

        assertTransactionClosed();
        assertEquals(0, new QDGenre().key.equalTo(genreKey).findCount());
    }

    @Test
    void deleteGenreCommitsAfterTheHandlerReturns() {
        DGenre existing = insertGenre();
        when(ctx.pathParamAsClass("id", Integer.class).get()).thenReturn(existing.getId());
        doAnswer(invocation -> {
            captureTransaction(false);
            assertNull(DB.find(DGenre.class, existing.getId()));
            return ctx;
        }).when(ctx).status(204);

        GenreHandler.deleteGenre(ctx);

        assertTransactionClosed();
        verify(ctx).status(204);
        assertNull(DB.find(DGenre.class, existing.getId()));
    }

    @Test
    void deleteGenreRollsBackDeletionWhenResponseFails() {
        DGenre existing = insertGenre();
        when(ctx.pathParamAsClass("id", Integer.class).get()).thenReturn(existing.getId());
        IllegalStateException failure = new IllegalStateException("Cannot set deletion status");
        doAnswer(invocation -> {
            captureTransaction(false);
            assertNull(DB.find(DGenre.class, existing.getId()));
            throw failure;
        }).when(ctx).status(204);

        assertSame(failure, assertThrows(IllegalStateException.class, () -> GenreHandler.deleteGenre(ctx)));

        assertTransactionClosed();
        DGenre retained = DB.find(DGenre.class, existing.getId());
        assertNotNull(retained);
        assertEquals(genreKey, retained.getKey());
    }

    @Test
    void addGenreClosesTransactionWhenValidationFails() {
        GenreToAdd input = new GenreToAdd().setName("").setKey(genreKey);
        when(ctx.req().getLocale()).thenReturn(Locale.JAPAN);
        doAnswer(invocation -> {
            captureTransaction(false);
            return input;
        }).when(ctx).bodyAsClass(GenreToAdd.class);

        assertThrows(BadRequestResponse.class, () -> GenreHandler.addGenre(ctx));

        assertTransactionClosed();
        assertEquals(0, new QDGenre().key.equalTo(genreKey).findCount());
    }

    @Test
    void deleteGenreClosesTransactionWhenGenreDoesNotExist() {
        DGenre removed = insertGenre();
        removed.delete();
        when(ctx.pathParamAsClass("id", Integer.class).get()).thenReturn(removed.getId());
        when(ctx.req().getLocale()).thenReturn(Locale.JAPAN);
        doAnswer(invocation -> {
            captureTransaction(false);
            return ctx;
        }).when(ctx).status(400);

        GenreHandler.deleteGenre(ctx);

        assertTransactionClosed();
        verify(ctx).status(400);
        assertNull(DB.find(DGenre.class, removed.getId()));
    }

    private void givenValidInput() {
        GenreToAdd input = new GenreToAdd().setName("TxGenre").setKey(genreKey).setOrderNumber(3);
        when(ctx.bodyAsClass(GenreToAdd.class)).thenReturn(input);
    }

    private DGenre insertGenre() {
        DGenre genre = new DGenre();
        genre.setName("TxGenre");
        genre.setKey(genreKey);
        genre.setOrderNumber(3);
        genre.save();
        return genre;
    }
}
