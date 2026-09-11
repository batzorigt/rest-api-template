package rest.api.member;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.ebean.DB;
import io.javalin.http.BadRequestResponse;
import rest.api.HandlerTransactionTestSupport;

class MemberHandlerTransactionTest extends HandlerTransactionTestSupport {

    private static final String NAME = "TxnMember";
    private static final List<String> PHONES = List.of("tx-member-1", "tx-member-2");

    @BeforeEach
    void prepareFixtures() {
        removeFixtures();
    }

    @Override
    protected void removeFixtures() {
        DB.find(DPhone.class).where().in("phoneNo", PHONES).delete();
        DB.find(DMember.class).where().eq("name", NAME).delete();
    }

    @Test
    void addMemberCommitsMemberAndAllCascadedPhones() {
        stubInput(new MemberToAdd(NAME, PHONES));
        AtomicReference<Member> response = new AtomicReference<>();
        doAnswer(invocation -> {
            assertSame(transaction, DB.currentTransaction());
            assertTrue(transaction.isActive());
            response.set(invocation.getArgument(0));
            assertStoredMemberAndPhones(response.get());
            return ctx;
        }).when(ctx).json(any(Member.class));

        MemberHandler.addMember(ctx);

        assertTransactionClosed();
        verify(ctx).status(HttpStatus.CREATED_201);
        assertStoredMemberAndPhones(response.get());
    }

    @Test
    void addMemberRollsBackMemberAndAllCascadedPhonesWhenResponseFails() {
        stubInput(new MemberToAdd(NAME, PHONES));
        AtomicReference<Member> response = new AtomicReference<>();
        IllegalStateException failure = new IllegalStateException("Cannot serialize member response");
        doAnswer(invocation -> {
            assertSame(transaction, DB.currentTransaction());
            assertTrue(transaction.isActive());
            response.set(invocation.getArgument(0));
            assertStoredMemberAndPhones(response.get());
            throw failure;
        }).when(ctx).json(any(Member.class));

        assertSame(failure, assertThrows(IllegalStateException.class, () -> MemberHandler.addMember(ctx)));

        assertTransactionClosed();
        assertNotNull(response.get());
        assertNull(DB.find(DMember.class, response.get().getId()));
        assertEquals(0, DB.find(DMember.class).where().eq("name", NAME).findCount());
        assertEquals(0, DB.find(DPhone.class).where().in("phoneNo", PHONES).findCount());
        for (Phone phone : response.get().getPhones()) {
            assertNull(DB.find(DPhone.class, phone.getId()));
        }
    }

    @Test
    void addMemberClosesTransactionWhenValidationFails() {
        stubInput(new MemberToAdd("", PHONES));

        assertThrows(BadRequestResponse.class, () -> MemberHandler.addMember(ctx));

        assertTransactionClosed();
        verify(ctx, never()).json(any());
        assertEquals(0, DB.find(DMember.class).where().eq("name", "").findCount());
        assertEquals(0, DB.find(DPhone.class).where().in("phoneNo", PHONES).findCount());
    }

    @Test
    void getMemberUsesReadOnlyTransactionAndClosesItAfterResponse() {
        assertReadOnlyTransactionDeclared(MemberHandler.class, "getMember");
        Member existing = MemberService.addMember(new MemberToAdd(NAME, PHONES));
        when(ctx.pathParamAsClass("id", Integer.class).get()).thenReturn(existing.getId());
        doAnswer(invocation -> {
            captureTransaction();
            Member response = invocation.getArgument(0);
            assertEquals(existing.getId(), response.getId());
            assertStoredMemberAndPhones(response);
            return ctx;
        }).when(ctx).json(any(Member.class));

        MemberHandler.getMember(ctx);

        assertTransactionClosed();
        verify(ctx).status(HttpStatus.OK_200);
        assertStoredMemberAndPhones(existing);
    }

    @Test
    void getMemberClosesReadOnlyTransactionWhenResponseFails() {
        assertReadOnlyTransactionDeclared(MemberHandler.class, "getMember");
        Member existing = MemberService.addMember(new MemberToAdd(NAME, PHONES));
        when(ctx.pathParamAsClass("id", Integer.class).get()).thenReturn(existing.getId());
        IllegalStateException failure = new IllegalStateException("Cannot serialize member response");
        doAnswer(invocation -> {
            captureTransaction();
            Member response = invocation.getArgument(0);
            assertEquals(existing.getId(), response.getId());
            throw failure;
        }).when(ctx).json(any(Member.class));

        assertSame(failure, assertThrows(IllegalStateException.class, () -> MemberHandler.getMember(ctx)));

        assertTransactionClosed();
        assertStoredMemberAndPhones(existing);
    }

    @Test
    void getMissingMemberClosesReadOnlyTransaction() {
        assertReadOnlyTransactionDeclared(MemberHandler.class, "getMember");
        when(ctx.pathParamAsClass("id", Integer.class).get()).thenReturn(Integer.MAX_VALUE);
        doAnswer(invocation -> {
            captureTransaction();
            return ctx;
        }).when(ctx).json(any());

        MemberHandler.getMember(ctx);

        assertTransactionClosed();
        verify(ctx).status(HttpStatus.NOT_FOUND_404);
        verify(ctx).json(any());
    }

    private void stubInput(MemberToAdd input) {
        when(ctx.bodyAsClass(MemberToAdd.class)).thenAnswer(invocation -> {
            captureTransaction(false);
            return input;
        });
    }

    private void assertStoredMemberAndPhones(Member response) {
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(NAME, response.getName());
        assertEquals(2, response.getPhones().size());
        assertEquals(Set.copyOf(PHONES), response.getPhones().stream().map(Phone::getPhoneNo)
                .collect(Collectors.toSet()));
        DMember stored = DB.find(DMember.class, response.getId());
        assertNotNull(stored);
        assertEquals(NAME, stored.getName());
        assertEquals(1, DB.find(DMember.class).where().eq("id", response.getId()).findCount());
        assertEquals(2, DB.find(DPhone.class).where().eq("member.id", response.getId()).findCount());
        for (Phone phone : response.getPhones()) {
            assertNotNull(phone.getId());
            DPhone storedPhone = DB.find(DPhone.class, phone.getId());
            assertNotNull(storedPhone);
            assertEquals(phone.getPhoneNo(), storedPhone.getPhoneNo());
            assertEquals(response.getId(), storedPhone.getMember().getId());
        }
    }
}
