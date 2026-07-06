package com.aislego.addresses.service;

import com.aislego.addresses.domain.Address;
import com.aislego.addresses.dto.AddressResponse;
import com.aislego.addresses.dto.CreateAddressRequest;
import com.aislego.addresses.repository.AddressRepository;
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
        Address address = new Address();
        address.setUser(userRepository.getReferenceById(userId));
        address.setLabel(request.label());
        address.setLine1(request.line1());
        address.setLine2(request.line2());
        address.setCity(request.city());
        address.setState(request.state());
        address.setPostalCode(request.postalCode());
        address.setLatitude(request.lat());
        address.setLongitude(request.lng());
        address.setDefault(request.isDefault());

        return AddressResponse.from(addressRepository.save(address));
    }
}
