package guru.springframework.brewery.web.controllers;

import guru.springframework.brewery.services.BeerOrderService;
import guru.springframework.brewery.web.model.BeerOrderDto;
import guru.springframework.brewery.web.model.BeerOrderLineDto;
import guru.springframework.brewery.web.model.BeerOrderPagedList;
import guru.springframework.brewery.web.model.OrderStatusEnum;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BeerOrderController.class)
class BeerOrderControllerTest {

    @MockBean
    BeerOrderService beerOrderService;

    @Autowired
    MockMvc mockMvc;

    BeerOrderDto validBeerOrder;

    @BeforeEach
    void setUp() {
        validBeerOrder = BeerOrderDto.builder()
                .version(1)
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .customerRef("BeerCustomer")
                .beerOrderLines(List.of(new BeerOrderLineDto()))
                .orderStatus(OrderStatusEnum.READY)
                .orderStatusCallbackUrl("localhost:8080/actuator")
                .createdDate(OffsetDateTime.now())
                .lastModifiedDate(OffsetDateTime.now())
                .build();
    }

    @AfterEach
    void tearDown() {
        reset(beerOrderService);
    }

    @Test
    void getOrder() throws Exception {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
        given(beerOrderService.getOrderById(any(), any())).willReturn(validBeerOrder);

        mockMvc.perform(get("/api/v1/customers/" + validBeerOrder.getCustomerId() + "/orders/" + validBeerOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(validBeerOrder.getId().toString())))
                .andExpect(jsonPath("$.customerId", is(validBeerOrder.getCustomerId().toString())))
                .andExpect(jsonPath("$.customerRef", is("BeerCustomer")))
                .andExpect(jsonPath("$.createdDate",
                        is(dateTimeFormatter.format(validBeerOrder.getCreatedDate()))));
    }

    @Nested
    @DisplayName("Test List Beer Orders")
    public class TestListBeerOrders {
        @Captor
        ArgumentCaptor<UUID> customerIdCaptor;

        @Captor
        ArgumentCaptor<PageRequest> pageRequestCaptor;

        BeerOrderPagedList beerOrderPagedList;

        @BeforeEach
        void setUp() {
            BeerOrderDto validBeerOrder2 = BeerOrderDto.builder()
                    .version(1)
                    .id(UUID.randomUUID())
                    .customerId(validBeerOrder.getCustomerId())
                    .customerRef("BeerCustomer")
                    .beerOrderLines(List.of(new BeerOrderLineDto()))
                    .orderStatus(OrderStatusEnum.READY)
                    .orderStatusCallbackUrl("localhost:8080/actuator")
                    .createdDate(OffsetDateTime.now())
                    .lastModifiedDate(OffsetDateTime.now())
                    .build();

            List<BeerOrderDto> beerOrders = new ArrayList<>();
            beerOrders.add(validBeerOrder);
            beerOrders.add(validBeerOrder2);

            beerOrderPagedList = new BeerOrderPagedList(beerOrders, PageRequest.of(1, 1), 2L);

            given(beerOrderService.listOrders(customerIdCaptor.capture(),
                    pageRequestCaptor.capture())).willReturn(beerOrderPagedList);
        }

        @Test
        void listOrders() throws Exception {
            mockMvc.perform(get("/api/v1/customers/" + validBeerOrder.getCustomerId() + "/orders").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].id", is(validBeerOrder.getId().toString())));
            ;
        }
    }


}