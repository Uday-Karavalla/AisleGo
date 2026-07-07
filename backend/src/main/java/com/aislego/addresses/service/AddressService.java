package com.aislego.addresses.service;

import com.aislego.addresses.domain.Address;
import com.aislego.addresses.dto.AddressResponse;
import com.aislego.addresses.dto.CreateAddressRequest;
import com.aislego.addresses.repository.AddressRepository;
import com.aislego.common.exception.NotFoundException;
import com.aislego.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> listMine(Long userId) {
        return addressRepository.findByUserIdOrderByIdAsc(userId).stream()
                .map(AddressResponse::from)
                .toList();
    }

    public AddressResponse create(Long userId, CreateAddressRequest request) {
        if (request.isDefault()) {
            clearExistingDefault(userId);
        }

        Address address = new Address();
        address.setUser(userRepository.getReferenceById(userId));
        applyRequest(address, request);

        return AddressResponse.from(addressRepository.save(address));
    }

    public AddressResponse update(Long userId, Long addressId, CreateAddressRequest request) {
        Address address = findOwned(userId, addressId);
        if (request.isDefault() && !address.isDefault()) {
            clearExistingDefault(userId);
        }
        applyRequest(address, request);

        return AddressResponse.from(addressRepository.save(address));
    }

    public void delete(Long userId, Long addressId) {
        Address address = findOwned(userId, addressId);
        addressRepository.delete(address);
    }

    /**
     * At most one address can be the default at a time - clearing every existing default
     * before a create/update sets a new one keeps that invariant true without needing a DB
     * constraint, the same "read then re-check" style already used for ownership scoping
     * elsewhere in this codebase.
     */
    private void clearExistingDefault(Long userId) {
        List<Address> currentDefaults = addressRepository.findByUserIdAndIsDefaultTrue(userId);
        currentDefaults.forEach(existing -> existing.setDefault(false));
        addressRepository.saveAll(currentDefaults);
    }

    private void applyRequest(Address address, CreateAddressRequest request) {
        address.setLabel(request.label());
        address.setLine1(request.line1());
        address.setLine2(request.line2());
        address.setCity(request.city());
        address.setState(request.state());
        address.setPostalCode(request.postalCode());
        address.setLatitude(request.lat());
        address.setLongitude(request.lng());
        address.setDefault(request.isDefault());
    }

    private Address findOwned(Long userId, Long addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new NotFoundException("Address " + addressId + " was not found"));
    }
}
