package com.aislego.addresses.service;

import com.aislego.addresses.domain.Address;
import com.aislego.addresses.dto.AddressResponse;
import com.aislego.addresses.dto.CreateAddressRequest;
import com.aislego.addresses.repository.AddressRepository;
import com.aislego.identity.domain.User;
import com.aislego.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AddressService addressService;

    private static final Long USER_ID = 7L;

    private User userReference;

    @BeforeEach
    void setUp() {
        userReference = new User();
        userReference.setId(USER_ID);
    }

    @Test
    void listMineMapsRepositoryResultsInOrder() {
        Address home = buildAddress(1L, "Home", true);
        Address work = buildAddress(2L, "Work", false);
        when(addressRepository.findByUserIdOrderByIdAsc(USER_ID)).thenReturn(List.of(home, work));

        List<AddressResponse> result = addressService.listMine(USER_ID);

        assertThat(result).extracting(AddressResponse::label).containsExactly("Home", "Work");
        assertThat(result.get(0).isDefault()).isTrue();
    }

    @Test
    void createSavesAnAddressOwnedByTheCurrentUser() {
        when(userRepository.getReferenceById(USER_ID)).thenReturn(userReference);
        when(addressRepository.save(any())).thenAnswer(invocation -> {
            Address saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        CreateAddressRequest request = new CreateAddressRequest(
                "Home", "221B Baker Street", null, "Bengaluru", "Karnataka", "560001", 12.97, 77.59, true);

        AddressResponse response = addressService.create(USER_ID, request);

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.label()).isEqualTo("Home");
        assertThat(response.line1()).isEqualTo("221B Baker Street");
        assertThat(response.postalCode()).isEqualTo("560001");
        assertThat(response.isDefault()).isTrue();
    }

    private Address buildAddress(Long id, String label, boolean isDefault) {
        Address address = new Address();
        address.setId(id);
        address.setUser(userReference);
        address.setLabel(label);
        address.setLine1("Some street");
        address.setCity("Bengaluru");
        address.setState("Karnataka");
        address.setPostalCode("560001");
        address.setDefault(isDefault);
        return address;
    }
}
