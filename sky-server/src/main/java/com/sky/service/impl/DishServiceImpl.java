package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 新增菜品
     * @param dishDTO
     */
    @Transactional
    public void save(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        // 菜品插入一条数据
        dishMapper.insert(dish);

        // 获取insert语句生成的主键值
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0){
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            // 口味插入N条数据
            dishFlavorMapper.insert(flavors);
        }
    }

    /**
     * 分页查询菜品
     * @param dishPageQueryDTO
     */
    public PageResult page(DishPageQueryDTO dishPageQueryDTO) {
        Page<DishVO> page = new Page<>(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        dishMapper.pageQuery(page, dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getRecords());
    }


    /**
     * 批量删除
     * @param ids
     */
    public void deleteByIds(List<Long> ids) {
        // 起售中的不能删
        Long onSaleCount = dishMapper.selectCount(
                Wrappers.<Dish>lambdaQuery()
                        .in(Dish::getId, ids)
                        .eq(Dish::getStatus, StatusConstant.ENABLE));
        if (onSaleCount > 0) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
        }

        // 套餐关联不能删
        List<Long> setmealIds = setmealMapper.getSetmealIdsByDishId(ids);
        if(setmealIds != null && setmealIds.size() > 0){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        dishMapper.deleteByIds(ids);
        dishFlavorMapper.delete(Wrappers.<DishFlavor>lambdaQuery()
                .in(DishFlavor::getDishId,ids));
    }


    /**
     * 根据id查询菜品详情
     * @param id
     * @return
     */
    public DishVO selectById(Long id) {
        // 根据id查菜品
        Dish dish = dishMapper.selectById(id);
        // 查口味
        List<DishFlavor> dishFlavors = dishFlavorMapper.selectList(Wrappers.<DishFlavor>lambdaQuery()
                .eq(DishFlavor::getDishId, id));
        DishVO dishVo = new DishVO();
        BeanUtils.copyProperties(dish,dishVo);
        dishVo.setFlavors(dishFlavors);
        // 转换为VO
        return dishVo;
    }

    /**
     * 起售停售
     * @param status
     * @param id
     */
    public void updateStatus(Integer status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.updateById(dish);
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    public List<Dish> getDishsBycateId(Long categoryId) {
        return dishMapper.selectList(new LambdaQueryWrapper<Dish>().eq(Dish::getCategoryId,categoryId));
    }

    /**
     * 更新菜品
     * @param dishDTO
     */
    @Transactional
    public void update(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        // 菜品更新一条数据
        dishMapper.updateById(dish);

        // 删除原来的口味
        dishFlavorMapper.delete(Wrappers.<DishFlavor>lambdaQuery()
                .eq(DishFlavor::getDishId, dishDTO.getId()));

        // 口味写入N条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setId(null);
                dishFlavor.setDishId(dishDTO.getId());
            });
            dishFlavorMapper.insert(flavors);
        }
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(dish.getStatus() != null,Dish::getStatus, dish.getStatus())
                .like(dish.getName() != null,Dish::getName, dish.getName())
                .eq(dish.getCategoryId() != null,Dish::getCategoryId, dish.getCategoryId()));

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.selectList(Wrappers.<DishFlavor>lambdaQuery()
                    .eq(d.getId() != null,DishFlavor::getDishId, d.getId()));

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

}