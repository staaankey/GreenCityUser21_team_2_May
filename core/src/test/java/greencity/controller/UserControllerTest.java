package greencity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import greencity.ModelUtils;
import greencity.TestConst;

import static greencity.constant.AppConstant.AUTHORIZATION;

import greencity.constant.AppConstant;
import greencity.constant.ErrorMessage;
import greencity.converters.UserArgumentResolver;
import greencity.dto.PageableAdvancedDto;
import greencity.dto.filter.FilterUserDto;
import greencity.dto.language.LanguageVO;
import greencity.dto.ubs.UbsTableCreationDto;
import greencity.dto.user.UserManagementUpdateDto;
import greencity.dto.user.UserManagementVO;
import greencity.dto.user.UserManagementViewDto;
import greencity.dto.user.UserProfileDtoRequest;
import greencity.dto.user.UserStatusDto;
import greencity.dto.user.UserUpdateDto;
import greencity.dto.user.UserVO;
import greencity.enums.EmailNotification;
import greencity.enums.Role;
import greencity.exception.exceptions.WrongIdException;
import greencity.exception.handler.CustomExceptionHandler;
import greencity.repository.UserRepo;
import greencity.service.UserService;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.modelmapper.ModelMapper;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserControllerTest {
    private static final String userLink = "/user";
    private MockMvc mockMvc;
    @InjectMocks
    private UserController userController;
    @Mock
    private UserService userService;
    @Mock
    private UserRepo userRepo;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders
            .standaloneSetup(userController)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver(),
                new UserArgumentResolver(userService, new ModelMapper()))
                .setControllerAdvice(new CustomExceptionHandler(new DefaultErrorAttributes()))
            .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void updateStatusTest() throws Exception {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testmail@gmail.com");

        String content = "{\n"
            + "  \"id\": 0,\n"
            + "  \"userStatus\": \"BLOCKED\"\n"
            + "}";

        mockMvc.perform(patch(userLink + "/status")
            .principal(principal)
            .contentType(MediaType.APPLICATION_JSON)
            .content(content))
            .andExpect(status().isOk());

        ObjectMapper mapper = new ObjectMapper();
        UserStatusDto userStatusDto =
            mapper.readValue(content, UserStatusDto.class);

        verify(userService).updateStatus(userStatusDto.getId(),
            userStatusDto.getUserStatus(), "testmail@gmail.com");
    }

    @Test
    void updateStatusBadRequestTest() throws Exception {
        mockMvc.perform(patch(userLink + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updateUserLastActivityTimeTest_isOk() throws Exception {
        Principal principal = mock(Principal.class);
        UserVO userVO = ModelUtils.TEST_USER_VO;

        when(userService.findByEmail(principal.getName())).thenReturn(userVO);

        LocalDateTime date = LocalDateTime.now();

        mockMvc.perform(put(userLink + "/updateUserLastActivityTime/" + date)
                        .principal(principal))
                .andExpect(status().isOk());
    }

    @Test
    void updateRoleTest() throws Exception {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testmail@gmail.com");

        String content = "{\n"
            + "  \"role\": \"ROLE_USER\"\n"
            + "}";

        mockMvc.perform(patch(userLink + "/1/role")
            .principal(principal)
            .contentType(MediaType.APPLICATION_JSON)
            .content(content))
            .andExpect(status().isOk());

        verify(userService).updateRole(1L, Role.ROLE_USER, "testmail@gmail.com");
    }

    @Test
    void updateRoleBadRequestForEmptyBodyTest() throws Exception {
        mockMvc.perform(patch(userLink + "/1/role")
            .contentType(MediaType.APPLICATION_JSON)
            .content(""))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getAllUsersTest() throws Exception {
        int pageNumber = 1;
        int pageSize = 20;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        mockMvc.perform(get(userLink + "/all?page=1"))
            .andExpect(status().isOk());

        verify(userService).findByPage(pageable);
    }

    @Test
    void getRolesTest() throws Exception {
        mockMvc.perform(get(userLink + "/roles"))
            .andExpect(status().isOk());

        verify(userService).getRoles();
    }

    @Test
    void getEmailNotificationsTest() throws Exception {
        mockMvc.perform(get(userLink + "/emailNotifications"))
            .andExpect(status().isOk());

        verify(userService).getEmailNotificationsStatuses();
    }

    @Test
    void getUsersByFilterTest() throws Exception {
        int pageNumber = 1;
        int pageSize = 20;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        String content = "{\n"
            + "  \"searchReg\": \"string\"\n"
            + "}";

        mockMvc.perform(post(userLink + "/filter?page=1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(content))
            .andExpect(status().isOk());

        ObjectMapper mapper = new ObjectMapper();
        FilterUserDto filterUserDto =
            mapper.readValue(content, FilterUserDto.class);

        verify(userService).getUsersByFilter(filterUserDto, pageable);
    }

    @Test
    void getUserByPrincipalTest() throws Exception {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testmail@gmail.com");

        mockMvc.perform(get(userLink)
            .principal(principal))
            .andExpect(status().isOk());

        verify(userService).getUserUpdateDtoByEmail("testmail@gmail.com");
    }

    @Test
    void updateUserTest() throws Exception {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testmail@gmail.com");

        String content = "{\n"
            + "  \"emailNotification\": \"DISABLED\",\n"
            + "  \"name\": \"String\"\n"
            + "}";

        ObjectMapper mapper = new ObjectMapper();
        UserUpdateDto userUpdateDto =
            mapper.readValue(content, UserUpdateDto.class);

        mockMvc.perform(patch(userLink)
            .principal(principal)
            .contentType(MediaType.APPLICATION_JSON)
            .content(content))
            .andExpect(status().isOk());

        verify(userService).update(userUpdateDto, "testmail@gmail.com");
    }

    @Test
    void getAvailableCustomShoppingListItemTest() throws Exception {
        String accessToken = "accessToken";
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, accessToken);
        mockMvc.perform(get(userLink + "/{userId}/{habitId}/custom-shopping-list-items/available", 1, 1)
            .headers(headers))
            .andExpect(status().isOk());

        verify(userService).getAvailableCustomShoppingListItems(1L, 1L);
    }

    @Test
    void getActivatedUsersAmountTest() throws Exception {
        mockMvc.perform(get(userLink + "/activatedUsersAmount"))
            .andExpect(status().isOk());

        verify(userService).getActivatedUsersAmount();
    }

    @Test
    void updateUserProfilePictureTest() throws Exception {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testmail@gmail.com");
        UserVO user = ModelUtils.getUserVO();

        MockMultipartFile base64File = new MockMultipartFile("base64", "base64",
                MediaType.TEXT_PLAIN_VALUE, "iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==".getBytes());

        MockMultipartFile imageFile = new MockMultipartFile("image", "image.png",
                MediaType.IMAGE_PNG_VALUE, "image data".getBytes());

        when(userService.updateUserProfilePicture(any(), any(), any())).thenReturn(user);

        MockMultipartHttpServletRequestBuilder builder =
                multipart(userLink + "/profilePicture");
        builder.with(request -> {
            request.setMethod("PATCH");
            return request;
        });

        mockMvc.perform(builder
                        .file(base64File)
                        .file(imageFile)
                        .principal(principal)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());
    }

    @Test
    void deleteUserProfilePictureTest() throws Exception {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("test@email.com");
        mockMvc.perform(patch(userLink + "/deleteProfilePicture")
            .principal(principal))
            .andExpect(status().isOk());

        verify(userService, times(1)).deleteUserProfilePicture("test@email.com");
    }

    @Test
    void getUserProfileInformationTest() throws Exception {
        mockMvc.perform(get(userLink + "/{userId}/profile/", 1))
            .andExpect(status().isOk());
        verify(userService).getUserProfileInformation(1L);
    }

    @Test
    void checkIfTheUserIsOnlineTest() throws Exception {
        mockMvc.perform(get(userLink + "/isOnline/{userId}/", 1))
            .andExpect(status().isOk());
        verify(userService).checkIfTheUserIsOnline(1L);
    }

    @Test
    void checkIfTheUserIsOnlineTest_IsNotFound() throws Exception {
        Long userId = 111L;

        doThrow(new WrongIdException(ErrorMessage.USER_NOT_FOUND_BY_ID + userId))
                .when(userService).checkIfTheUserIsOnline(userId);

        mockMvc.perform(get(userLink + "/isOnline/{userId}/", userId))
                .andExpect(status().isNotFound());
    }
  
    @Test
    void checkIfTheUserIsOnlineTest_IsBadRequest() throws Exception {
        mockMvc.perform(get(userLink + "/isOnline/{userId}/", "badRequest"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(userService);
    }

    @Test
    void getUserProfileStatistics() throws Exception {
        String accessToken = "accessToken";
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, accessToken);
        mockMvc.perform(get(userLink + "/{userId}/profileStatistics/", 1)
            .headers(headers))
            .andExpect(status().isOk());
        verify(userService).getUserProfileStatistics((1L));
    }

    @Test
    void saveTest() throws Exception {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testName");

        String json = "{\n"
            + "\t\"name\": \"testName\",\n"
            + "\t\"city\": \"city\",\n"
            + "\t\"userCredo\": \"credo\",\n"
            + "\t\"showLocation\": true,\n"
            + "\t\"showEcoPlace\": true,\n"
            + "\t\"showShoppingList\": false\n"
            + "}";
        String accessToken = "accessToken";
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, accessToken);

        this.mockMvc.perform(put(userLink + "/profile")
            .headers(headers)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json)
            .param("accessToken", "accessToken")
            .principal(principal))
            .andExpect(status().isOk());

        ObjectMapper mapper = new ObjectMapper();
        UserProfileDtoRequest dto = mapper.readValue(json, UserProfileDtoRequest.class);

        verify(userService).saveUserProfile(dto, "testName");
    }

    @Test
    void searchTest() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        UserManagementViewDto userViewDto =
            UserManagementViewDto.builder()
                .id("1L")
                .name("vivo")
                .email("test@ukr.net")
                .userCredo("Hello")
                .role("1")
                .userStatus("1")
                .build();
        String content = objectMapper.writeValueAsString(userViewDto);
        List<UserManagementVO> userManagementVOS = Collections.singletonList(new UserManagementVO());
        PageableAdvancedDto<UserManagementVO> userAdvancedDto =
            new PageableAdvancedDto<>(userManagementVOS, 20, 0, 0, 0,
                true, true, true, true);
        when(userService.search(pageable, userViewDto)).thenReturn(userAdvancedDto);
        mockMvc.perform(post(userLink + "/search")
            .content(content)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        verify(userService).search(pageable, userViewDto);
    }

    @Test
    void findByEmailTest() throws Exception {
        UserVO userVO = ModelUtils.getUserVO();
        when(userService.findByEmail(TestConst.EMAIL)).thenReturn(userVO);
        mockMvc.perform(get(userLink + "/findByEmail")
            .param("email", TestConst.EMAIL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value(TestConst.NAME))
            .andExpect(jsonPath("$.email").value(TestConst.EMAIL));
    }

    @Test
    void findByIdTest() throws Exception {
        UserVO userVO = ModelUtils.getUserVO();
        when(userService.findById(1L)).thenReturn(userVO);
        mockMvc.perform(get(userLink + "/findById")
            .param("id", "1"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value(TestConst.NAME))
            .andExpect(jsonPath("$.email").value(TestConst.EMAIL));
    }

    @Test
    void findUserForManagementTest() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        when(userService.findUserForManagementByPage(pageable)).thenReturn(ModelUtils.getPageableAdvancedDto());
        mockMvc.perform(get(userLink + "/findUserForManagement"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.length()").value(1))
            .andExpect(jsonPath("$.totalElements").value(1L))
            .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void findUserForManagementByPage_isBadRequest() throws Exception {
        mockMvc.perform(get(userLink + "/findUserForManagement?sort=notExist,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("notExist property not exist"));
    }

    @Test
    void searchByTest() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        String query = "testQuery";
        when(userService.searchBy(pageable, query)).thenReturn(ModelUtils.getPageableAdvancedDto());
        mockMvc.perform(get(userLink + "/searchBy")
            .param("query", query))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.length()").value(1))
            .andExpect(jsonPath("$.totalElements").value(1L))
            .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void updateUserManagementTest() throws Exception {
        UserManagementUpdateDto userManagementDto = ModelUtils.getUserManagementUpdateDto();
        String content = objectMapper.writeValueAsString(userManagementDto);
        mockMvc.perform(put(userLink + "/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(content))
            .andExpect(status().isOk());
    }

    @Test
    void updateUserManagementBadRequestTest() throws Exception {
        mockMvc.perform(put(userLink + "/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(""))
            .andExpect(status().isBadRequest());
        verify(userService, times(0)).updateUser(1L, ModelUtils.getUserManagementUpdateDto());
    }

    @Test
    void findAllTest() throws Exception {
        when(userService.findAll()).thenReturn(List.of(ModelUtils.getUserVO()));
        mockMvc.perform(get(userLink + "/findAll"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value(TestConst.NAME))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].email").value(TestConst.EMAIL));
    }

    @Test
    void createUbsRecordTest() throws Exception {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn(TestConst.EMAIL);
        when(userService.findByEmail(principal.getName())).thenReturn(ModelUtils.getUserVO());

        when(userService.createUbsRecord(ModelUtils.getUserVO())).thenReturn(UbsTableCreationDto.builder()
            .uuid("testUuid")
            .build());
        mockMvc.perform(get(userLink + "/createUbsRecord")
            .principal(principal)
            .content(objectMapper.writeValueAsString(ModelUtils.getUserVO())))
            .andExpect(status().isOk())
            .andDo(MockMvcResultHandlers.print())
            .andExpect(jsonPath("$.uuid").value("testUuid"));

    }

    @Test
    void findIdByEmailTest() throws Exception {
        when(userService.findIdByEmail(TestConst.EMAIL)).thenReturn(1L);
        mockMvc.perform(get(userLink + "/findIdByEmail")
            .param("email", TestConst.EMAIL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(1L));
    }

    @Test
    void updateUserLanguageTest() throws Exception {
        Principal principal = mock(Principal.class);
        String languageCode = AppConstant.DEFAULT_LANGUAGE_CODE;
        long userId = 1L;
        UserVO userVO = UserVO.builder()
            .id(userId)
            .languageVO(LanguageVO.builder()
                .id(2L)
                .code(languageCode)
                .build())
            .build();

        when(principal.getName()).thenReturn(TestConst.EMAIL);
        when(userService.findByEmail(principal.getName())).thenReturn(userVO);

        mockMvc.perform(put(userLink + "/language/{languageId}", 1)
            .principal(principal))
            .andExpect(status().isOk());

        verify(userService).updateUserLanguage(userId, 1L);
    }

    @Test
    void getUserLang() throws Exception {
        Principal principal = mock(Principal.class);
        String languageCode = AppConstant.DEFAULT_LANGUAGE_CODE;
        UserVO userVO = ModelUtils.TEST_USER_VO;
        userVO.setLanguageVO(LanguageVO.builder()
            .id(2L)
            .code(languageCode)
            .build());

        when(principal.getName()).thenReturn(TestConst.EMAIL);
        when(userService.findByEmail(principal.getName())).thenReturn(userVO);

        this.mockMvc.perform(get(userLink + "/lang" + "?id=1")
            .principal(principal))
            .andExpect(content().string(languageCode))
            .andExpect(status().isOk());
    }

    @Test
    void getReasonsOfDeactivation() throws Exception {
        List<String> test = List.of("test", "test");
        when(userService.getDeactivationReason(1L, "en")).thenReturn(test);
        this.mockMvc.perform(get(userLink + "/reasons" + "?id=1" + "&admin=en")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        verify(userService).getDeactivationReason(1L, "en");
    }

    @Test
    void deactivateAllUserTest() throws Exception {
        List<Long> ids = List.of(1L, 2L, 3L, 4L);
        when(userService.deactivateAllUsers(ids)).thenReturn(ids);
        mockMvc.perform(put(userLink + "/deactivateAll")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(ids)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4));

    }

    @Test
    void saveUserTest() throws Exception {
        when(userService.save(ModelUtils.getUserVO())).thenReturn(ModelUtils.getUserVO());
        mockMvc.perform(post(userLink)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(ModelUtils.getUserVO())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value(TestConst.NAME))
            .andExpect(jsonPath("$.email").value(TestConst.EMAIL));
    }

    @Test
    void findAllByEmailNotificationTest() throws Exception {
        EmailNotification notification = EmailNotification.DAILY;
        when(userService.findAllByEmailNotification(notification))
            .thenReturn(List.of(ModelUtils.getUserVO()));
        mockMvc.perform(get(userLink + "/findAllByEmailNotification")
            .param("emailNotification", notification.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1L));

    }

    @Test
    void scheduleDeleteDeactivateUserTest() throws Exception {
        when(userService.scheduleDeleteDeactivatedUsers()).thenReturn(1);

        mockMvc.perform(post(userLink + "/deleteDeactivatedUsers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(1));
    }

    @Test
    void findAllUsersCitiesTest() throws Exception {
        List<String> cities = List.of("Lviv", "Kyiv", "Kharkiv");
        when(userService.findAllUsersCities()).thenReturn(cities);
        mockMvc.perform(get(userLink + "/findAllUsersCities"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$", Matchers.containsInAnyOrder("Lviv", "Kyiv", "Kharkiv")));
    }
}
