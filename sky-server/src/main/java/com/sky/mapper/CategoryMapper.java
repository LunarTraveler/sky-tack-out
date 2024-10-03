package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper // 虽然不加也是可以的，但是加上帮助编译器快速找到定位的位置和更好的开发人员
public interface CategoryMapper {

    /**
     * 新增分类
     * (如果要用mybatis的话那么单表的简单查询就用注解写就行了)
     * @param category
     */
    @Insert("insert into sky_take_out.category (type,name,sort,status,create_time,update_time,create_user,update_user)" +
            "values" +
            "(#{type}, #{name}, #{sort}, #{status}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    @AutoFill(value = OperationType.INSERT)
    void insert(Category category);

    /**
     * 分页查询
     */
    Page<Category> pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 根据id删除分类
     * @param id
     */
    @Delete("delete from sky_take_out.category where id = #{id}")
    void deleteById(Long id);

    /**
     * 修改分类
     * @param category
     * @return
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Category category);

    /**
     * 根据类型查询分类(注意只有启用的才能查到)
     * @param type
     * @return
     */
    List<Category> list(Integer type);
}
