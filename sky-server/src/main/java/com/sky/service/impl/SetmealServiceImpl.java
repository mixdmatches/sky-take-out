package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 修改套餐
     * @param setmealDTO
     */
    @Transactional
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        // 更新套餐表
        setmealMapper.updateById(setmeal);

        // 删除原来的菜品
        setmealDishMapper.delete(Wrappers.<SetmealDish>lambdaQuery().
                eq(SetmealDish::getSetmealId,setmealDTO.getId()));

        // 批量插入套餐菜品
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if(setmealDishes != null && !setmealDishes.isEmpty()){
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setId(null);
                setmealDish.setSetmealId(setmealDTO.getId());
            });
            setmealDishMapper.insert(setmealDishes);
        }
    }

    /**
     * 分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    public PageResult page(SetmealPageQueryDTO setmealPageQueryDTO) {
        Page<SetmealVO> page = new Page<>(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        setmealMapper.pageQuery(page, setmealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getRecords());
    }

    /**
     * 添加套餐
     * @param setmealDTO
     */
    @Transactional
    public void addSetmeal(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.insert(setmeal);

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if(setmealDishes != null && !setmealDishes.isEmpty()){
            Long setmealId = setmeal.getId();
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealId);
            });
            setmealDishes.forEach(setmealDishMapper::insert);
        }

    }


    /**
     * 获取单个套餐详情
     * @param id
     */
    public SetmealVO getById(Long id) {
        // 查套餐
        Setmeal setmeal = setmealMapper.selectById(id);

        LambdaQueryWrapper<SetmealDish> lqw = new LambdaQueryWrapper<>();
        lqw.eq(SetmealDish::getSetmealId,id);

        // 查跟套餐关联的 套餐菜品
        List<SetmealDish> setmealDishes = setmealDishMapper.selectList(lqw);

        // 封装进一个对象
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal,setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);

        return setmealVO;
    }

    /**
     * 批量删除套餐
     * @param ids
     */
    @Transactional
    public void deleteByIds(List<Long> ids) {
        Long onSaleCount = setmealMapper.selectCount(Wrappers.<Setmeal>lambdaQuery()
                .in(Setmeal::getId,ids)
                .eq(Setmeal::getStatus, StatusConstant.ENABLE));
        if(onSaleCount > 0 ){
            throw new DeletionNotAllowedException("起售中不能删除");
        }

        setmealMapper.deleteByIds(ids);
        setmealDishMapper.delete(Wrappers.<SetmealDish>lambdaQuery()
                .in(SetmealDish::getSetmealId, ids));

    }

    /**
     * 套餐起售、停售
     * @param status
     */
    public void updateStatus(Integer status,Long id) {
        Setmeal setmeal = Setmeal.builder().status(status).id(id).build();
        setmealMapper.updateById(setmeal);
    }
}