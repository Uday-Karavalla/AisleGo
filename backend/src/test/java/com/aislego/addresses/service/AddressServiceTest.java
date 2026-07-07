package com.aislego.addresses.service;

import com.aislego.addresses.domain.Address;
import com.aislego.addresses.dto.AddressResponse;
import com.aislego.addresses.dto.CreateAddressRequest;
import com.aislego.addresses.repository.AddressRepository;
import com.aislego.common.exception.NotFoundException;
import com.aislego.identity.domain.User;
import com.aislego.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    @Test
    void createUnsetsAnyExistingDefaultWhenTheNewAddressIsDefault() {
        Address existingDefault = buildAddress(1L, "Home", true);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(userReference);
        when(addressRepository.findByUserIdAndIsDefaultTrue(USER_ID)).thenReturn(List.of(existingDefault));
        when(addressRepository.save(any())).thenAnswer(invocation -> {
            Address saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        CreateAddressRequest request = new CreateAddressRequest(
                "Work", "1 Office Park", null, "Bengaluru", "Karnataka", "560002", null, null, true);

        addressService.create(USER_ID, request);

        assertThat(existingDefault.isDefault()).isFalse();
        verify(addressRepository).saveAll(List.of(existingDefault));
    }

    @Test
    void updateModifiesAnOwnedAddress() {
        Address existing = buildAddress(5L, "Home", false);
        when(addressRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(existing));
        when(addressRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreateAddressRequest request = new CreateAddressRequest(
                "Home (updated)", "New street", null, "Bengaluru", "Karnataka", "560003", null, null, false);

        AddressResponse response = addressService.update(USER_ID, 5L, request);

        assertThat(response.label()).isEqualTo("Home (updated)");
        assertThat(response.line1()).isEqualTo("New street");
    }

    @Test
    void updateThrowsNotFoundWhenTheAddressBelongsToAnotherCustomer() {
        when(addressRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.empty());

        CreateAddressRequest request = new CreateAddressRequest(
                "Home", "Street", null, "Bengaluru", "Karnataka", "560001", null, null, false);

        assertThatThrownBy(() -> addressService.update(USER_ID, 5L, request))
                .isInstanceOf(NotFoundException.class);
        verify(addressRepository, never()).save(any());
    }

    @Test
    void deleteRemovesAnOwnedAddress() {
        Address existing = buildAddress(5L, "Home", false);
        when(addressRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(existing));

        addressService.delete(USER_ID, 5L);

        verify(addressRepository).delete(existing);
    }

    @Test
    void deleteThrowsNotFoundWhenTheAddressBelongsToAnotherCustomer() {
        when(addressRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.delete(USER_ID, 5L))
                .isInstanceOf(NotFoundException.class);
        verify(addressRepository, never()).delete(any(Address.class));
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
