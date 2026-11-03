package com.marcos.userservice;

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
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void crudFlow() throws Exception {
        var email = UUID.randomUUID() + "@test.com";
        var body = mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"name\":\"Ana\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mvc.perform(get("/api/v1/users/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Ana"));

        mvc.perform(put("/api/v1/users/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Bea\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Bea"));

        mvc.perform(delete("/api/v1/users/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/users/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void invalidEmailIsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nope\",\"name\":\"Ana\"}"))
                .andExpect(status().isBadRequest());
    }
}
