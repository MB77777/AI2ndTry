package eu.pp.mb.test.ai2ndtry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FrontendController.class)
class FrontendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRedirectSailbankToFrontendIndex() throws Exception {
        mockMvc.perform(get("/sailbank"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sailbank/index.html"));
    }
}
