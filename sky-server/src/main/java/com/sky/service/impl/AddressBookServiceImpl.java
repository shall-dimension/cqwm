package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.exception.AddressBookBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class AddressBookServiceImpl implements AddressBookService {

    @Autowired
    private AddressBookMapper addressBookMapper;

    /**
     * 条件查询
     *
     * @param addressBook 查询条件
     * @return 地址列表
     */
    @Override
    public List<AddressBook> list(AddressBook addressBook) {
        return addressBookMapper.list(addressBook);
    }

    /**
     * 新增地址
     *
     * @param addressBook 地址信息
     */
    @Override
    public void save(AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBook.setIsDefault(0);
        addressBookMapper.insert(addressBook);
    }

    /**
     * 根据id查询当前用户的地址
     *
     * @param id 地址id
     * @return 地址信息
     */
    @Override
    public AddressBook getById(Long id) {
        return getCurrentUserAddress(id);
    }

    /**
     * 根据id修改当前用户的地址
     *
     * @param addressBook 地址信息
     */
    @Override
    public void update(AddressBook addressBook) {
        getCurrentUserAddress(addressBook.getId());
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBookMapper.update(addressBook);
    }

    /**
     * 设置默认地址
     *
     * @param addressBook 地址信息
     */
    @Override
    @Transactional
    public void setDefault(AddressBook addressBook) {
        getCurrentUserAddress(addressBook.getId());

        // 将当前用户的所有地址修改为非默认地址
        addressBook.setIsDefault(0);
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBookMapper.updateIsDefaultByUserId(addressBook);

        // 将指定地址修改为默认地址
        addressBook.setIsDefault(1);
        addressBookMapper.update(addressBook);
    }

    /**
     * 根据id删除当前用户的地址
     *
     * @param id 地址id
     */
    @Override
    public void deleteById(Long id) {
        getCurrentUserAddress(id);
        addressBookMapper.deleteById(id);
    }

    /**
     * 查询并校验地址是否属于当前用户
     *
     * @param id 地址id
     * @return 地址信息
     */
    private AddressBook getCurrentUserAddress(Long id) {
        AddressBook addressBook = addressBookMapper.getById(id);
        Long currentUserId = BaseContext.getCurrentId();
        if (addressBook == null || !currentUserId.equals(addressBook.getUserId())) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_NOT_FOUND);
        }
        return addressBook;
    }
}
