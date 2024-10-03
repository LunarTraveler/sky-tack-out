package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.DishFlavor;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    /**
     * 插入口味
     * @param flavors
     */
    @AutoFill(OperationType.INSERT)
    void insertBatch(List<DishFlavor> flavors);

    @Delete("delete from sky_take_out.dish_flavor where dish_id = #{dishId}")
    void deleteByDishId(Long dishId);

    void deleteBatchByDishId(List<Long> ids);

    @Select("select * from sky_take_out.dish_flavor where dish_id = #{dishId}")
    List<DishFlavor> selectByDisId(Long dishId);
}
