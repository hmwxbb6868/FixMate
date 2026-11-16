package com.fixmate.config;

import com.fixmate.exception.BusinessException;
import com.fixmate.security.StaffIdentityGuard;
import com.fixmate.service.admin.StaffAdminProvisioner;
import com.fixmate.service.admin.serviceImpl.StaffAuthServiceImpl;
import java.io.Console;
import java.util.Arrays;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** 本机交互式初始化/无其他ADMIN时恢复；无HTTP注册、默认密码或命令行密码。 */
@Component
@ConditionalOnProperty(name="fixmate.bootstrap-admin.enabled",havingValue="true")
public class StaffAdminBootstrap implements ApplicationRunner {
    private final StaffAdminProvisioner provisioner;
    private final ConfigurableApplicationContext context;
    private final Environment environment;
    public StaffAdminBootstrap(StaffAdminProvisioner provisioner,ConfigurableApplicationContext context,Environment environment) {
        this.provisioner=provisioner;this.context=context;this.environment=environment;
    }
    @Override public void run(ApplicationArguments args) {
        if(!"-1".equals(environment.getProperty("server.port")))throw new IllegalStateException("初始化必须显式指定 --server.port=-1");
        Console console=System.console();if(console==null)throw new IllegalStateException("须在本机交互式终端运行，禁止重定向密码");
        String mode=environment.getProperty("fixmate.bootstrap-admin.mode","create");
        if(!Set.of("create","recover").contains(mode))throw BusinessException.validation("初始化模式不正确");
        String username=StaffAuthServiceImpl.normalized(console.readLine("Admin username: "));
        StaffIdentityGuard.text(username,3,64,"用户名");
        String name="create".equals(mode)?console.readLine("Admin display name: "):null;
        if("recover".equals(mode)&&!"RESET".equals(console.readLine("Type RESET to recover the sole enabled administrator: ")))
            throw BusinessException.validation("恢复已取消");
        char[] first=console.readPassword("New initial password (exactly 6 digits): ");
        char[] confirmation=console.readPassword("Confirm password: ");
        try {
            if(first==null||confirmation==null||!Arrays.equals(first,confirmation))throw BusinessException.validation("两次密码不一致");
            provisioner.provision(mode,username,name,new String(first));
            console.printf("Admin credentials saved. Change the initial password at first login.%n");
        } finally {
            if(first!=null)Arrays.fill(first,'\0');if(confirmation!=null)Arrays.fill(confirmation,'\0');context.close();
        }
    }
}
