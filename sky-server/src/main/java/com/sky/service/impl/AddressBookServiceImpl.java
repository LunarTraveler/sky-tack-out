package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.mapper.AddressBookMapper;
import com.sky.service.AddressBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AddressBookServiceImpl implements AddressBookService {

    private final AddressBookMapper addressBookMapper;

    /**
     * 查询当前登录的用户的所有地址信息
     * @param addressBookFilter
     * @return
     */
    @Override
    public List<AddressBook> list(AddressBook addressBookFilter) {
        return addressBookMapper.list(addressBookFilter);
    }

    /**
     * 新增地址
     * @param addressBook
     */
    @Override
    public void save(AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBook.setIsDefault(0);
        addressBookMapper.insert(addressBook);
    }

    /**
     * 根据id查询地址
     * @param id
     * @return
     */
    @Override
    public AddressBook getById(Long id) {
        return addressBookMapper.selectById(id);
    }

    /**
     * 根据id删除地址
     * @param id
     */
    @Override
    public void deleteById(Long id) {
        addressBookMapper.deleteById(id);
    }

    /**
     * 根据id修改地址
     * @param addressBook
     */
    @Override
    public void update(AddressBook addressBook) {
        addressBookMapper.update(addressBook);
    }

    /**
     * 设置默认地址
     * @param addressBook
     */
    @Override
    public void setDefault(AddressBook addressBook) {
        // 先把所有的地址都设置能不是
        AddressBook addressBookFilter = AddressBook.builder()
                .userId(BaseContext.getCurrentId())
                .isDefault(0)
                .build();
        addressBookMapper.updateIsDefaultByUserId(addressBookFilter);

        // 再把传来的那个id地址设置为默认的地址
        addressBookFilter.setIsDefault(1);
        addressBookMapper.update(addressBookFilter);
    }
}
