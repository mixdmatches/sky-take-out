package com.sky.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时支付订单
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processOrder() {
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        List<Orders> ordersList = orderMapper.selectList(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getStatus,Orders.PENDING_PAYMENT)
                .le(Orders::getOrderTime,time));
        if (ordersList != null && !ordersList.isEmpty()){
            for(Orders orders:ordersList){
                orders.setCancelTime(LocalDateTime.now());
                orders.setCancelReason("订单超时自动取消");
                orders.setStatus(Orders.CANCELLED);
                orderMapper.update(orders);
            }
        }
    }

    /**
     * 处理一直处于派送中的订单
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder(){
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        List<Orders> ordersList = orderMapper.selectList(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getStatus,Orders.DELIVERY_IN_PROGRESS)
                .le(Orders::getOrderTime,time));
        if (ordersList != null && !ordersList.isEmpty()){
            for(Orders orders:ordersList){
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            }
        }
    }
}
