package com.sky.controller.admin;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.dto.PasswordEditDTO;
import com.sky.entity.Employee;
import com.sky.exception.BaseException;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.utils.JwtUtil;
import com.sky.vo.EmployeeLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @PostMapping("/login")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build();

        return Result.success(employeeLoginVO);
    }

    /**
     * 修改密码
     * @param passwordEditDTO
     * @return
     */
    @PutMapping("/editPassword")
    public Result editPassword(@RequestBody PasswordEditDTO passwordEditDTO){
        employeeService.editPassword(passwordEditDTO);
        return Result.success();
    }

    /**
     * 分页查询
     * @param employeePageQueryDTO
     * @return
     */
    @GetMapping("/page")
    public Result<PageResult> getPageEmpl(EmployeePageQueryDTO employeePageQueryDTO) {
        PageResult pr = employeeService.getPageEmpl(employeePageQueryDTO);
        return Result.success(pr);
    }

    /**
     * 添加员工
     * @param employeeDTO
     * @return
     */
    @PostMapping
    public Result addEmpl(@RequestBody EmployeeDTO employeeDTO){
        String phone = employeeDTO.getPhone();
        String idNumber = employeeDTO.getIdNumber();
        if(phone != null && phone.length() > 11){
            throw  new BaseException("手机号长度不能超过11位");
        }
        if(idNumber != null && idNumber.length() > 18) throw new BaseException("身份证号长度超过18位");
        employeeService.addEmpl(employeeDTO);
        return Result.success();
    }

    /**
     * 启用禁用员工状态
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    public Result changeStatus(@PathVariable Integer status, @RequestParam Long id){
        employeeService.changeStatus(status,id);
        return Result.success();
    }

    /**
     * 修改员工信息
     * @param employeeDTO
     * @return
     */
    @PutMapping
    public Result updateEmployee(@RequestBody EmployeeDTO employeeDTO){
        log.info("修改员工：{}",employeeDTO);
        employeeService.updateEmployee(employeeDTO);
        return Result.success();
    }

    /**
     * 根据id获取员工信息
     * @param id
     * @return
     */
    @GetMapping("{id}")
    public Result<Employee> getById(@PathVariable Long id){
        Employee employee = employeeService.getById(id);
        return Result.success(employee);
    }

    /**
     * 退出
     *
     * @return
     */
    @PostMapping("/logout")
    public Result<String> logout() {
        return Result.success();
    }

}