package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    /**
     * 通过从菜品的ids找到对应的套餐的ids
     * @param ids
     * @return
     */
    List<Long> getSetmealDishIdsByDishId(List<Long> ids);

    /**
     * 批量增加套餐与菜品的的对应关系
     * @param setmealDishes
     */
    void insertBatch(List<SetmealDish> setmealDishes);

    /**
     * 批量删除餐与菜品的的对应关系
     * @param setmealIds
     */
    void deleteBatch(List<Long> setmealIds);

    @Delete("delete from sky_take_out.setmeal_dish where setmeal_id = #{setmealId}")
    void deleteBySetmealId(Long setmealId);

    /**
     * 根据套餐id获取到所有对应的  套餐-菜品
     * @param id
     * @return
     */
    @Select("select * from sky_take_out.setmeal_dish where setmeal_id = #{id}")
    List<SetmealDish> getBySetmealId(Long id);
}
