package greencity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import greencity.ModelUtils;
import greencity.config.PageableConfig;
import greencity.config.SecurityConfig;
import greencity.dto.user.UserStatusDto;
import greencity.repository.UserRepo;
import greencity.security.jwt.JwtTool;
import greencity.service.EmailService;
import greencity.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static greencity.enums.UserStatus.DEACTIVATED;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {SecurityConfig.class, UserController.class, PageableConfig.class})
@WebAppConfiguration
@EnableWebMvc
@Import({JwtTool.class})
@TestPropertySource(properties = {
        "accessTokenValidTimeInMinutes=60",
        "refreshTokenValidTimeInMinutes=1440",
        "tokenKey=secretTokenKey"
})

@ActiveProfiles("dev")
public class UserControllerWithSecurityConfigTest {
    private static final String userLink = "/user";

    private MockMvc mockMvc;

    private UserController userController;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepo userRepo;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        when(userService.findByEmail(anyString())).thenReturn(ModelUtils.getUserVO());
    }

    @Test
    @WithMockUser(username = "testmail@gmail.com", roles = "USER")
    void getUserByPrincipal_isForbidden() throws Exception {
        mockMvc.perform(get(userLink))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "testmail@gmail.com", roles = "USER")
    void getEmailNotificationsTest_isOk() throws Exception {
        mockMvc.perform(get(userLink + "/emailNotifications"))
                .andExpect(status().isOk());

        verify(userService).getEmailNotificationsStatuses();
    }

    @Test
    @WithAnonymousUser
    void getEmailNotificationsTest_isUnauthorized() throws Exception {
        mockMvc.perform(get(userLink + "/emailNotifications")
                        .with(anonymous()))
                .andExpect(status().isUnauthorized());
        verify(userService, times(0)).getEmailNotificationsStatuses();
    }

    @Test
    @WithMockUser(username = "testmail@mail.com", roles = "ADMIN")
    void deleteUserProfilePicture_isOk() throws Exception {
        mockMvc.perform(patch(userLink + "/deleteProfilePicture"))
                .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void updateUserProfilePicture_isUnauthorized() throws Exception {
        mockMvc.perform(patch(userLink + "/profilePicture")
                        .with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "Admin", roles = "ADMIN")
    void checkIfTheUserIsOnline_isOk() throws Exception {
        mockMvc.perform(get(userLink + "/isOnline/{userId}/", 1L))
                .andExpect(status().isOk());
        verify(userService).checkIfTheUserIsOnline(1L);
    }

    @Test
    @WithMockUser(username = "Admin", roles = "ADMIN")
    void checkIfTheUserIsOnline_isBadRequest() throws Exception {
        mockMvc.perform(get(userLink + "/isOnline/{userId}/", "badRequest"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "User", roles = "USER")
    void checkIfTheUserIsOnline_isForbidden() throws Exception {
        mockMvc.perform(get(userLink + "/isOnline/{userId}/", 1L))
                .andExpect(status().isForbidden());
        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "testAdmicMail@gmail.com", roles = "ADMIN")
    void updateStatusTest_isOk() throws Exception {
        UserStatusDto responseUserStatusDto = new UserStatusDto();
        responseUserStatusDto.setId(1L);
        responseUserStatusDto.setUserStatus(DEACTIVATED);

        when(userService.updateStatus(1L, DEACTIVATED, "testAdmicMail@gmail.com"))
                .thenReturn(responseUserStatusDto);

        String content = "{\n"
                + "  \"id\": 0,\n"
                + "  \"userStatus\": \"DEACTIVATED\"\n"
                + "}";

        mockMvc.perform(patch(userLink + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isOk());

        ObjectMapper mapper = new ObjectMapper();
        UserStatusDto userStatusDto =
                mapper.readValue(content, UserStatusDto.class);

        verify(userService).updateStatus(userStatusDto.getId(),
                userStatusDto.getUserStatus(), "testAdmicMail@gmail.com");
    }

    @Test
    @WithAnonymousUser
    void updateStatusTest_isUnauthorized() throws Exception {
        mockMvc.perform(get(userLink + "/status"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "testmail@mail.com", roles = "USER")
    void findUserForManagementByPage_isForbidden() throws Exception {
        mockMvc.perform(get(userLink + "/findUserForManagement"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "User", roles = "USER")
    void updateStatusTest_isForbidden() throws Exception {
        String content = "{\n"
                + "  \"id\": 0,\n"
                + "  \"userStatus\": \"DEACTIVATED\"\n"
                + "}";

        mockMvc.perform(patch(userLink + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "Admin", roles = "ADMIN")
    void updateStatusTest_isBadRequest() throws Exception {
        mockMvc.perform(patch(userLink + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "Admin", roles = "ADMIN")
    void updateUserLastActivityTimeTest_IsOk() throws Exception {
        LocalDateTime date = LocalDateTime.now();

        mockMvc.perform(put(userLink + "/updateUserLastActivityTime/" + date))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource({
            "User, USER",
            "Moderator, MODERATOR",
            "Ubs_Employee, UBS_EMPLOYEE",
            "Employee, EMPLOYEE"
    })
    @WithMockUser
    void updateUserLastActivityTimeTest_IsForbiddenForNonAdmins(String username, String role) throws Exception {
        LocalDateTime date = LocalDateTime.now();

        mockMvc.perform(put(userLink + "/updateUserLastActivityTime/" + date)
                        .with(user(username).roles(role)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "Admin", roles = "ADMIN")
    void getUsersByFilterTest_isOk() throws Exception {
        String content = "{\n"
                + "  \"searchReg\": \"string\"\n"
                + "}";

        mockMvc.perform(post(userLink + "/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content)
                        .param("page", "0"))
                .andExpect(status().isOk());
    }
}
