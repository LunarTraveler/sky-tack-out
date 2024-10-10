package com.sky.mapper;

import com.sky.entity.AddressBook;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AddressBookMapper {

    /**
     * 动态查询所有的地址信息
     * @param addressBookFilter
     * @return
     */
    List<AddressBook> list(AddressBook addressBookFilter);

    /**
     * 新增地址
     * @param addressBook
     */
    @Insert("insert into " +
            "sky_take_out.address_book(user_id, consignee, sex, phone, province_code, province_name, city_code, city_name, " +
            "district_code, district_name, detail, label, is_default) " +
            "VALUES " +
            "(#{userId},#{consignee},#{sex},#{phone},#{provinceCode},#{provinceName},#{cityCode},#{cityName}," +
            "#{districtCode},#{districtName},#{detail},#{label},#{isDefault})"
    )
    void insert(AddressBook addressBook);

    /**
     * 根据id查询地址
     * @param id
     * @return
     */
    @Select("select * from sky_take_out.address_book where id = #{id}")
    AddressBook selectById(Long id);

    /**
     * 根据id删除地址
     * @param id
     */
    @Delete("delete from sky_take_out.address_book where id = #{id}")
    void deleteById(Long id);

    /**
     * 根据id修改地址
     * @param addressBook
     */
    void update(AddressBook addressBook);

    /**
     * 设置默认地址
     * @param addressBookFilter
     */
    @Update("update sky_take_out.address_book set is_default = #{isDefault} where user_id = #{userId}")
    void updateIsDefaultByUserId(AddressBook addressBookFilter);
}
