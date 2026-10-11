package com.example.campusapp.Data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;

import com.example.campusapp.Data.local.dao.StudentDao;
import com.example.campusapp.Data.local.entities.StudentEntity;
import com.example.campusapp.Data.remote.ApiClient;
import com.example.campusapp.Data.remote.ApiService;
import com.example.campusapp.Data.repository.RealStudentRepository;
import com.example.campusapp.model.Result;
import com.example.campusapp.model.StudentPage;
import com.example.campusapp.util.AppExecutors;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
public class RealStudentRepositoryTest {

    private static final String ONE_STUDENT_JSON =
            "{\"students\":[{\"student_id\":\"1\",\"student_number\":\"100000001\","
                    + "\"full_name\":\"Chileshe Banda\",\"programme\":\"CS\","
                    + "\"confirmed_group\":\"G01\",\"version\":1}],"
                    + "\"page\":1,\"hasMore\":false}";

    private MockWebServer server;
    private StudentDao dao;
    private RealStudentRepository repo;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        dao = mock(StudentDao.class);
        ApiService api = ApiClient.create(
                server.url("/").toString(),
                chain -> chain.proceed(chain.request()));
        repo = new RealStudentRepository(api, dao, AppExecutors.get());
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    /** Calls loadPage and waits for the callback, which is delivered on the main looper. */
    private Result<StudentPage> load(long requestId, String query, int page) throws Exception {
        AtomicReference<Result<StudentPage>> out = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        repo.loadPage(requestId, query, page, 20, (id, result) -> {
            out.set(result);
            done.countDown();
        });
        long deadline = System.currentTimeMillis() + 5000;
        while (done.getCount() > 0 && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(10);
        }
        return out.get();
    }

    @Test
    public void http200_returnsStudentsAndCachesThem() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(ONE_STUDENT_JSON));

        Result<StudentPage> r = load(1, "", 1);

        assertEquals(Result.Status.SUCCESS, r.status);
        assertEquals(1, r.data.students.size());
        assertEquals("Chileshe Banda", r.data.students.get(0).name);
        verify(dao, timeout(2000)).upsertAll(anyList());
    }

    @Test
    public void http401_reportsSessionExpired() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(401));

        assertEquals(Result.Status.SESSION_EXPIRED, load(2, "", 1).status);
    }

    @Test
    public void http403_reportsNotAllowed() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(403));

        assertEquals(Result.Status.NOT_ALLOWED, load(3, "", 1).status);
    }

    @Test
    public void http500_reportsError() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(500));

        assertEquals(Result.Status.ERROR, load(4, "", 1).status);
    }

    @Test
    public void noConnection_returnsCachedRowsAsOffline() throws Exception {
        server.shutdown(); // nothing listens any more, so the call fails with an IOException

        StudentEntity cached = new StudentEntity();
        cached.studentId = "1";
        cached.studentNumber = "100000001";
        cached.fullName = "Chileshe Banda";
        cached.programme = "CS";
        cached.confirmedGroup = "G01";
        cached.version = 1;
        List<StudentEntity> rows = Collections.singletonList(cached);
        when(dao.searchCached(anyString(), anyInt(), anyInt())).thenReturn(rows);

        Result<StudentPage> r = load(5, "", 1);

        assertEquals(Result.Status.OFFLINE, r.status);
        assertEquals(1, r.data.students.size());
        assertFalse(r.data.hasMore);
    }

    @Test
    public void cancelledRequest_neverCallsBack() throws Exception {
        // The reply is delayed, so the cancel happens while the request is still in flight.
        server.enqueue(new MockResponse()
                .setBody(ONE_STUDENT_JSON)
                .setBodyDelay(5, TimeUnit.SECONDS));
        AtomicInteger calls = new AtomicInteger();

        repo.loadPage(6, "", 1, 20, (id, result) -> calls.incrementAndGet());
        repo.cancel(6);

        for (int i = 0; i < 50; i++) {
            shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(10);
        }
        assertEquals(0, calls.get());
    }
}