package dev.ccosta.aisha.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.backup.SystemBackupCoordinator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class SystemBackupWriteBlockInterceptorTest {

    @Mock
    private SystemBackupCoordinator backupCoordinator;

    @Test
    void shouldBlockDataChangingRequestsWhenBackupIsRunning() throws Exception {
        when(backupCoordinator.backupRunning()).thenReturn(true);
        SystemBackupWriteBlockInterceptor interceptor = new SystemBackupWriteBlockInterceptor(backupCoordinator);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/entries");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(503);
    }

    @Test
    void shouldAllowBackupRequestsWhenBackupIsRunning() throws Exception {
        SystemBackupWriteBlockInterceptor interceptor = new SystemBackupWriteBlockInterceptor(backupCoordinator);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/system-backup/jobs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldAllowReadRequestsWhenBackupIsRunning() throws Exception {
        SystemBackupWriteBlockInterceptor interceptor = new SystemBackupWriteBlockInterceptor(backupCoordinator);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/entries");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
