package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("admin/dish")
@Api(tags = "菜品接口")
@Slf4j
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 用于统一清理redis的缓存数据
     * @param pattern
     */
    private void cacheClean(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }

    /**
     * 新增菜品和对应的口味
     * @param dishDTO
     * @return
     */
    @ApiOperation("新增菜品和对应的口味")
    @PostMapping()
    public Result saveWithFlavor(@RequestBody DishDTO dishDTO) {
        dishService.saveWithFlavor(dishDTO);

        // 清理缓存数据，因为这个分类新增了一个菜品，前台展示的时候是没有这条数据的，所以要全部清理(但是只是涉及一个分类)
        String key = "dish_" + dishDTO.getCategoryId();
        cacheClean(key);

        return Result.success();
    }

    /**
     * 菜品分页展示
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("菜品分页展示")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        PageResult pageResult = dishService.page(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 删除菜品 字符串使用逗号隔开解析成list集合
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("批量删除菜品")
    public Result delete(@RequestParam List<Long> ids) {
        dishService.deleteBatch(ids);
        // 可能会涉及多个分类，为了不查询数据库，所以全部删除
        cacheClean("dish_*");
        return Result.success();
    }

    /**
     * 根据id获取相应的菜品信息以及口味信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id获取相应的菜品信息以及口味信息")
    public Result<DishVO> getDishById(@PathVariable Long id) {
        DishVO dishVO = dishService.getDishById(id);
        return Result.success(dishVO);
    }

    /**
     * 修改菜品信息
     * @param dishDTO
     * @return
     */
    @PutMapping()
    @ApiOperation("修改菜品信息")
    public Result updateDishWithFlavor(@RequestBody DishDTO dishDTO) {
        dishService.updateDishWithFlavor(dishDTO);
        // 可能会涉及一个或两个分类，为了不查询数据库，所以全部删除
        cacheClean("dish_*");
        return Result.success();
    }

    /**
     * 禁用启用功能
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("启用禁用分类")
    public Result<String> startOrStop(@PathVariable("status") Integer status, Long id){
        dishService.startOrStop(status,id);
        // 这里好像只涉及一个分类吧(但是这个分类要查询数据库...)
        cacheClean("dish_*");
        return Result.success();
    }

    /**
     * 根据分类id查询菜品
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> list(Long categoryId) {
        List<Dish> dishList = dishService.list(categoryId);
        return Result.success(dishList);
    }


}
