package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询套餐的数量
     * @param categoryId
     * @return
     */
    @Select("select count(1) from sky_take_out.dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 插入菜品
     * @param dish
     */
    @AutoFill(OperationType.INSERT)
    void insert(Dish dish);

    /**
     * 菜品分页展示
     * @param dishPageQueryDTO
     * @return
     */
    Page<DishVO> page(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 根据id查询相应的菜品
     * @param id
     * @return
     */
    @Select("select * from sky_take_out.dish where id = #{id}")
    Dish selectById(Long id);

    /**
     * 根据id删除相应的菜品
     * @param dishId
     */
    @Delete("delete from sky_take_out.dish where id = #{dishId}")
    void delete(Long dishId);

    /**
     * 根据ids批量删除相应的菜品
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     * 修改dish表的信息 (修改都是动态修改就行)
     * @param dish
     */
    @AutoFill(OperationType.UPDATE)
    void update(Dish dish);

}
