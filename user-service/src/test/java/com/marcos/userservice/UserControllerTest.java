package com.marcos.userservice;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.UUID;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void crudFlow() throws Exception {
        var username = "u" + UUID.randomUUID();
        var email = username + "@test.com";
        var body = mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"name\":\"Ana\",\"username\":\"" + username
                                + "\",\"password\":\"password1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mvc.perform(get("/api/v1/users/" + id)).andExpect(status().isUnauthorized());
        mvc.perform(login(username, "wrong-pass")).andExpect(status().isUnauthorized());

        mvc.perform(login(email, "password1")).andExpect(status().isOk());
        var token = "Bearer " + JsonPath.read(
                mvc.perform(login(username, "password1")).andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), "$.accessToken");

        mvc.perform(get("/api/v1/users/" + id).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Ana"));

        mvc.perform(put("/api/v1/users/" + id).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Bea\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Bea"));

        mvc.perform(delete("/api/v1/users/" + id).header("Authorization", token)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/users/" + id).header("Authorization", token)).andExpect(status().isNotFound());
    }

    @Test
    void onlyOwnerOrAdminCanTouchAUser() throws Exception {
        var a = "a" + UUID.randomUUID();
        var b = "b" + UUID.randomUUID();
        var idA = register(a);
        var idB = register(b);
        var tokenA = "Bearer " + JsonPath.read(
                mvc.perform(login(a, "password1")).andReturn().getResponse().getContentAsString(), "$.accessToken");

        mvc.perform(get("/api/v1/users/" + idA).header("Authorization", tokenA)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/users/" + idB).header("Authorization", tokenA)).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/users/" + idB).header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\"}")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/users/" + idB).header("Authorization", tokenA)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/users").header("Authorization", tokenA)).andExpect(status().isForbidden());

        var admin = jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
        mvc.perform(get("/api/v1/users").with(admin)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/users/" + idB).with(admin)).andExpect(status().isOk());
    }

    private String register(String username) throws Exception {
        var body = mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + username + "@test.com\",\"name\":\"N\",\"username\":\"" + username
                                + "\",\"password\":\"password1\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private static MockHttpServletRequestBuilder login(String identifier, String password) {
        return post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"" + identifier + "\",\"password\":\"" + password + "\"}");
    }

    @Test
    void invalidEmailIsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nope\",\"name\":\"Ana\",\"username\":\"ana\",\"password\":\"password1\"}"))
                .andExpect(status().isBadRequest());
    }
}
