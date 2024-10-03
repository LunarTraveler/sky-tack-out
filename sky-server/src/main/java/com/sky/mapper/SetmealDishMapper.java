package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    /**
     * 通过从菜品的ids找到对应的套餐的ids
     * @param ids
     * @return
     */
    List<Long> getSetmealDishIdsByDishId(List<Long> ids);

}
