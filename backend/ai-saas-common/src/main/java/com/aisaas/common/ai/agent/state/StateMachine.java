package com.aisaas.common.ai.agent.state;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 状态机
 * 管理工作流状态的转换规则
 */
@Slf4j
public class StateMachine {

    /**
     * 状态转换规则图
     * Map<当前状态, Map<事件, 目标状态>>
     */
    private final Map<String, Map<String, String>> transitions = new ConcurrentHashMap<>();

    /**
     * 状态监听器
     */
    private final Map<String, StateListener> listeners = new ConcurrentHashMap<>();

    public StateMachine() {
        initializeDefaultTransitions();
    }

    /**
     * 初始化默认状态转换规则
     */
    private void initializeDefaultTransitions() {
        // PENDING 状态转换
        addTransition(WorkflowState.PENDING.getCode(), "START", WorkflowState.RUNNING.getCode());
        addTransition(WorkflowState.PENDING.getCode(), "CANCEL", WorkflowState.CANCELLED.getCode());

        // RUNNING 状态转换
        addTransition(WorkflowState.RUNNING.getCode(), "PAUSE", WorkflowState.PAUSED.getCode());
        addTransition(WorkflowState.RUNNING.getCode(), "COMPLETE", WorkflowState.COMPLETED.getCode());
        addTransition(WorkflowState.RUNNING.getCode(), "FAIL", WorkflowState.FAILED.getCode());
        addTransition(WorkflowState.RUNNING.getCode(), "CANCEL", WorkflowState.CANCELLED.getCode());
        addTransition(WorkflowState.RUNNING.getCode(), "TIMEOUT", WorkflowState.TIMEOUT.getCode());
        addTransition(WorkflowState.RUNNING.getCode(), "WAIT_INPUT", WorkflowState.WAITING_FOR_INPUT.getCode());
        addTransition(WorkflowState.RUNNING.getCode(), "WAIT_SUBFLOW", WorkflowState.WAITING_FOR_SUBFLOW.getCode());
        addTransition(WorkflowState.RUNNING.getCode(), "WAIT_EVENT", WorkflowState.WAITING_FOR_EVENT.getCode());

        // PAUSED 状态转换
        addTransition(WorkflowState.PAUSED.getCode(), "RESUME", WorkflowState.RUNNING.getCode());
        addTransition(WorkflowState.PAUSED.getCode(), "CANCEL", WorkflowState.CANCELLED.getCode());

        // 等待状态转换
        addTransition(WorkflowState.WAITING_FOR_INPUT.getCode(), "INPUT_RECEIVED", WorkflowState.RUNNING.getCode());
        addTransition(WorkflowState.WAITING_FOR_INPUT.getCode(), "CANCEL", WorkflowState.CANCELLED.getCode());

        addTransition(WorkflowState.WAITING_FOR_SUBFLOW.getCode(), "SUBFLOW_COMPLETED", WorkflowState.RUNNING.getCode());
        addTransition(WorkflowState.WAITING_FOR_SUBFLOW.getCode(), "CANCEL", WorkflowState.CANCELLED.getCode());

        addTransition(WorkflowState.WAITING_FOR_EVENT.getCode(), "EVENT_RECEIVED", WorkflowState.RUNNING.getCode());
        addTransition(WorkflowState.WAITING_FOR_EVENT.getCode(), "CANCEL", WorkflowState.CANCELLED.getCode());

        // FAILED 和 TIMEOUT 状态转换
        addTransition(WorkflowState.FAILED.getCode(), "RETRY", WorkflowState.RUNNING.getCode());
        addTransition(WorkflowState.TIMEOUT.getCode(), "RETRY", WorkflowState.RUNNING.getCode());
    }

    /**
     * 添加状态转换规则
     */
    public void addTransition(String currentState, String event, String nextState) {
        transitions.computeIfAbsent(currentState, k -> new HashMap<>()).put(event, nextState);
    }

    /**
     * 移除状态转换规则
     */
    public void removeTransition(String currentState, String event) {
        Map<String, String> stateTransitions = transitions.get(currentState);
        if (stateTransitions != null) {
            stateTransitions.remove(event);
        }
    }

    /**
     * 获取下一个状态
     */
    public String getNextState(String currentState, String event) {
        Map<String, String> stateTransitions = transitions.get(currentState);
        if (stateTransitions != null) {
            return stateTransitions.get(event);
        }
        return null;
    }

    /**
     * 检查状态转换是否有效
     */
    public boolean isValidTransition(String currentState, String event) {
        return getNextState(currentState, event) != null;
    }

    /**
     * 执行状态转换
     */
    public String transition(String currentState, String event) {
        String nextState = getNextState(currentState, event);
        if (nextState != null) {
            log.debug("State transition: {} --[{}]--> {}", currentState, event, nextState);

            // 触发状态变更监听器
            StateListener listener = listeners.get(nextState);
            if (listener != null) {
                listener.onStateEnter(nextState, currentState, event);
            }

            StateListener exitListener = listeners.get(currentState);
            if (exitListener != null) {
                exitListener.onStateExit(currentState, nextState, event);
            }

            return nextState;
        }
        throw new IllegalStateException(
                String.format("Invalid state transition: %s --[%s]--> ?", currentState, event));
    }

    /**
     * 注册状态监听器
     */
    public void registerListener(String state, StateListener listener) {
        listeners.put(state, listener);
    }

    /**
     * 移除状态监听器
     */
    public void removeListener(String state) {
        listeners.remove(state);
    }

    /**
     * 获取所有可能的事件
     */
    public Set<String> getPossibleEvents(String state) {
        Map<String, String> stateTransitions = transitions.get(state);
        if (stateTransitions != null) {
            return stateTransitions.keySet();
        }
        return java.util.Collections.emptySet();
    }

    /**
     * 状态监听器接口
     */
    public interface StateListener {
        /**
         * 进入状态时触发
         */
        void onStateEnter(String state, String previousState, String event);

        /**
         * 离开状态时触发
         */
        void onStateExit(String state, String nextState, String event);
    }

    /**
     * 状态转换适配器
     */
    public abstract static class StateListenerAdapter implements StateListener {
        @Override
        public void onStateEnter(String state, String previousState, String event) {
        }

        @Override
        public void onStateExit(String state, String nextState, String event) {
        }
    }
}
