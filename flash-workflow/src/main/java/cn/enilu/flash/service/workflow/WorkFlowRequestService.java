package cn.enilu.flash.service.workflow;


import cn.enilu.flash.bean.entity.workflow.WorkFlowRequest;
import cn.enilu.flash.bean.vo.query.SearchFilter;
import cn.enilu.flash.dao.workflow.WorkFlowRequestRepository;
import cn.enilu.flash.service.BaseService;
import cn.enilu.flash.utils.JsonUtil;
import cn.enilu.flash.utils.Lists;
import cn.enilu.flash.utils.factory.Page;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkFlowRequestService extends BaseService<WorkFlowRequest, Long, WorkFlowRequestRepository> {
    private Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private WorkFlowRequestRepository workFlowRequestRepository;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private TaskService taskService;

    /**
     * 启动新流程
     * 1，保存业务表单
     * 2，启动新流程
     * 3，关联流程信息和业务信息
     * @param flowRequest
     * @return
     */
    @Override
    public WorkFlowRequest insert(WorkFlowRequest flowRequest) {
        //1,
        flowRequest.setState(0);
        flowRequest = super.insert(flowRequest);
        //2,
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(flowRequest.getProcessDefId()).singleResult();
        String key = processDefinition.getKey();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(key, flowRequest.getId().toString());
        //3,
        flowRequest.setProcessDefName(processDefinition.getName());
        logger.info("id:{}", processInstance.getId());
        logger.info("processInstanceId:{}", processInstance.getProcessInstanceId());
        flowRequest.setInstanceId(processInstance.getProcessInstanceId());
        update(flowRequest);
        return flowRequest;
    }

    /**
     * 查询所有代办任务
     * @return
     */
    public Page<WorkFlowRequest> queryTask(Page<WorkFlowRequest> page,List<String> roleNames){
        TaskQuery taskQuery = taskService.createTaskQuery();
        List<Task> tasks = taskQuery.taskAssigneeIds(roleNames).listPage(page.getOffset(),page.getLimit());
        Long count = taskQuery.count();
        page.setTotal(count.intValue());
        List<WorkFlowRequest> flowRequests = Lists.newArrayList();
        for(Task task:tasks){
            String processInstanceId = task.getProcessInstanceId();
            WorkFlowRequest flowRequest = get(SearchFilter.build("instanceId",processInstanceId));
            flowRequests.add(flowRequest);
            flowRequest.setTaskId(task.getId());
        }
        page.setRecords(flowRequests);
        logger.info(JsonUtil.toJson(page));
        return page;
    }


}
