package customer.incident_management;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.hamcrest.Matchers.hasSize;
import com.jayway.jsonpath.JsonPath;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Application.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IncidentsODataTests {

    private static final String incidentsURI = "/odata/v4/ProcessorService/Incidents";
    private static final String customerURI = "/odata/v4/ProcessorService/Customers";
    private static final String expandEntityURI = "/odata/v4/ProcessorService/Customers?$select=firstName&$expand=incidents";
    private static final String adminCustomerURI = "/odata/v4/AdminService/Customers";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private String draftId;
    private String incidentId;

    @BeforeAll
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // -- GET Endpoints

    @Test @Order(1) @WithMockUser(username = "alice")
    void shouldCheckProcessorService() throws Exception {
        mockMvc.perform(get(incidentsURI))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.value", hasSize(4)));
    }

    @Test @Order(2) @WithMockUser(username = "alice")
    void shouldCheckCustomers() throws Exception {
        mockMvc.perform(get(customerURI))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.value", hasSize(3)));
    }

    @Test @Order(3) @WithMockUser(username = "alice")
    void testExpandEntityEndpoint() throws Exception {
        mockMvc.perform(get(expandEntityURI))
            .andExpect(jsonPath("$.value[0].incidents[0]").isMap())
            .andExpect(jsonPath("$.value[0].incidents[0]").isNotEmpty());
    }

    // -- Draft Choreography APIs

    @Test @Order(4) @WithMockUser(username = "alice")
    void createAnIncident() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post(incidentsURI)
            .content("{ \"title\": \"Urgent attention required !\", \"status_code\": \"N\" }")
            .contentType("application/json")
            .accept("application/json"))
            .andExpect(status().isCreated())
            .andReturn();
        draftId = JsonPath.read(result.getResponse().getContentAsString(), "$.ID");
        incidentId = draftId;
    }

    @Test @Order(5) @WithMockUser(username = "alice")
    void activateDraftAndCheckUrgencyH() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(incidentsURI+"(ID="+draftId+",IsActiveEntity=false)/ProcessorService.draftActivate")
            .contentType("application/json")
            .accept("application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.urgency_code").value("H"));
    }

    @Test @Order(6) @WithMockUser(username = "alice")
    void testIncidentStatus() throws Exception {
        mockMvc.perform(get(incidentsURI+"(ID="+incidentId+",IsActiveEntity=true)"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status_code").value("N"));
    }

    @Test @Order(7) @WithMockUser(username = "alice")
    void shouldCloseTheIncident() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(incidentsURI+"(ID="+incidentId+",IsActiveEntity=true)/ProcessorService.draftEdit")
            .content("{ \"PreserveChanges\": true }")
            .contentType("application/json")
            .accept("application/json"))
            .andExpect(status().isOk());
    }

    @Test @Order(8) @WithMockUser(username = "alice")
    void shouldPatchIncidentStatusToClosed() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch(incidentsURI+"(ID="+incidentId+",IsActiveEntity=false)")
            .content("{\"status_code\": \"C\"}")
            .contentType("application/json")
            .accept("application/json"))
            .andExpect(status().isOk());
    }

    @Test @Order(9) @WithMockUser(username = "alice")
    void activateDraftAndCheckStatusC() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(incidentsURI+"(ID="+incidentId+",IsActiveEntity=false)/ProcessorService.draftActivate")
            .contentType("application/json")
            .accept("application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status_code").value("C"));
    }

    @Test @Order(10) @WithMockUser(username = "alice")
    void testIncidentStatusToBeClosed() throws Exception {
        mockMvc.perform(get(incidentsURI+"(ID="+incidentId+",IsActiveEntity=true)"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status_code").value("C"));
    }

    @Test @Order(11) @WithMockUser(username = "alice")
    void shouldOpenClosedIncident() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(incidentsURI+"(ID="+incidentId+",IsActiveEntity=true)/ProcessorService.draftEdit")
            .content("{ \"PreserveChanges\": true }")
            .contentType("application/json")
            .accept("application/json"))
            .andExpect(status().isOk());
    }

    @Test @Order(12) @WithMockUser(username = "alice")
    void shouldReOpenTheIncidentButFail() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch(incidentsURI+"(ID="+incidentId+",IsActiveEntity=false)")
            .content("{\"status_code\": \"N\"}")
            .contentType("application/json")
            .accept("application/json"))
            .andExpect(status().isOk());
    }

    @Test @Order(13) @WithMockUser(username = "alice")
    void shouldFailToActivateDraftTryingToReOpenIncident() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(incidentsURI+"(ID="+incidentId+",IsActiveEntity=false)/ProcessorService.draftActivate")
            .contentType("application/json")
            .accept("application/json"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.message").value("Can't modify a closed incident"));
    }

    @Test @Order(14) @WithMockUser(username = "alice")
    void deleteTheDraft() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(incidentsURI+"(ID="+incidentId+",IsActiveEntity=false)"))
            .andExpect(status().is(204));
    }

    @Test @Order(15) @WithMockUser(username = "alice")
    void deleteTheIncident() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(incidentsURI+"(ID="+incidentId+",IsActiveEntity=true)"))
            .andExpect(status().is(204));
    }

    // -- Auto-Urgency logic

    @Test @Order(16) @WithMockUser(username = "alice")
    void doesNotChangeUrgencyForNonUrgentTitle() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post(incidentsURI)
            .content("{ \"title\": \"Routine maintenance\", \"urgency_code\": \"L\", \"status_code\": \"N\" }")
            .contentType("application/json")
            .accept("application/json"))
            .andExpect(status().isCreated())
            .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.ID");

        mockMvc.perform(MockMvcRequestBuilders.post(incidentsURI+"(ID="+id+",IsActiveEntity=false)/ProcessorService.draftActivate")
            .contentType("application/json").accept("application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.urgency_code").value("L"));

        mockMvc.perform(MockMvcRequestBuilders.delete(incidentsURI+"(ID="+id+",IsActiveEntity=true)"))
            .andExpect(status().is(204));
    }

    @Test @Order(17) @WithMockUser(username = "alice")
    void setsUrgencyHForAllCapsUrgentTitle() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post(incidentsURI)
            .content("{ \"title\": \"URGENT: system failure\", \"urgency_code\": \"L\", \"status_code\": \"N\" }")
            .contentType("application/json")
            .accept("application/json"))
            .andExpect(status().isCreated())
            .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.ID");

        mockMvc.perform(MockMvcRequestBuilders.post(incidentsURI+"(ID="+id+",IsActiveEntity=false)/ProcessorService.draftActivate")
            .contentType("application/json").accept("application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.urgency_code").value("H"));

        mockMvc.perform(MockMvcRequestBuilders.delete(incidentsURI+"(ID="+id+",IsActiveEntity=true)"))
            .andExpect(status().is(204));
    }

    @Test @Order(18) @WithMockUser(username = "alice")
    void setsUrgencyHForUrgentInMidTitle() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post(incidentsURI)
            .content("{ \"title\": \"Please treat this as urgent matter\", \"urgency_code\": \"M\", \"status_code\": \"N\" }")
            .contentType("application/json")
            .accept("application/json"))
            .andExpect(status().isCreated())
            .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.ID");

        mockMvc.perform(MockMvcRequestBuilders.post(incidentsURI+"(ID="+id+",IsActiveEntity=false)/ProcessorService.draftActivate")
            .contentType("application/json").accept("application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.urgency_code").value("H"));

        mockMvc.perform(MockMvcRequestBuilders.delete(incidentsURI+"(ID="+id+",IsActiveEntity=true)"))
            .andExpect(status().is(204));
    }

    // -- Authorization

    @Test @Order(19) @WithMockUser(username = "alice")
    void rejectsSupportRoleUserFromAdminServiceWith403() throws Exception {
        mockMvc.perform(get(adminCustomerURI))
            .andExpect(status().isForbidden());
    }

    @Test @Order(20) @WithMockUser(username = "bob")
    void allowsAdminRoleUserToReadAndWriteCustomers() throws Exception {
        mockMvc.perform(get(adminCustomerURI))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.value").isArray());

        mockMvc.perform(MockMvcRequestBuilders.post(adminCustomerURI)
            .content("{ \"ID\": \"AUTH01\", \"firstName\": \"Test\", \"lastName\": \"Admin\" }")
            .contentType("application/json")
            .accept("application/json"))
            .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.delete(adminCustomerURI+"('AUTH01')"))
            .andExpect(status().is(204));
    }
}
