package com.mapreduce;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * @param <InputMapKey>       Map阶段输入key的类型
 * @param <InputMapValue>     Map阶段输入value的类型
 * @param <IntermediateKey>   Map阶段输出key的类型
 * @param <IntermediateValue> Map阶段输出value的类型
 */
public class InputData<InputMapKey extends Comparable<InputMapKey>, InputMapValue, IntermediateKey extends Comparable<IntermediateKey>, IntermediateValue> {
    List<KeyValue<InputMapKey, InputMapValue>> initialKeyValue;
    List<KeyValue<IntermediateKey, IntermediateValue>> mappedKeyValue;
    List<GroupedKeyValue<IntermediateKey, IntermediateValue>> gKVList;
    List<KeyValue<IntermediateKey, IntermediateValue>> list = new ArrayList<KeyValue<IntermediateKey, IntermediateValue>>();
    private static Logger logger = LogManager.getLogger(InputData.class.getName());


    InputData() {
        this.initialKeyValue = new ArrayList<KeyValue<InputMapKey, InputMapValue>>();
        this.mappedKeyValue = new LinkedList<KeyValue<IntermediateKey, IntermediateValue>>();
        this.gKVList = new ArrayList<GroupedKeyValue<IntermediateKey, IntermediateValue>>();
    }
    public List<KeyValue<IntermediateKey, IntermediateValue>> instanceMapedKeyValue(){
        return new LinkedList<KeyValue<IntermediateKey, IntermediateValue>>();
    }
    void putKeyValue(InputMapKey key, InputMapValue value) {
        if(initialKeyValue==null){
            initialKeyValue = new ArrayList<KeyValue<InputMapKey, InputMapValue>>();
            initialKeyValue.add(new KeyValue<InputMapKey, InputMapValue>(key, value));
        }else{
            this.initialKeyValue.add(new KeyValue<InputMapKey, InputMapValue>(key, value));
        }
    }

    /**
     * @param inputlist
     */
    void reloadKeyValueList(List<KeyValue<InputMapKey, InputMapValue>> inputlist) {
        this.initialKeyValue = inputlist;
    }


    /**
     * @param index
     * @return
     */
    InputMapKey getMapKey(int index) {
        return this.initialKeyValue.get(index).getKey();
    }

    InputMapValue getMapValue(int index) {
        return this.initialKeyValue.get(index).getValue();
    }

    int getMapSize() {
        return this.initialKeyValue.size();
    }

    int getMappedKeyValueSize(){
        return this.mappedKeyValue.size();
    }


    void setMap(IntermediateKey k, IntermediateValue v) {

            this.mappedKeyValue.add(new KeyValue<IntermediateKey, IntermediateValue>(k, v));
    }

    /*
     * 显示Map阶段后的键值对
     */
    void showMap() {
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i).getKey().toString() + "," + list.get(i).getValue().toString());

        }
    }

	
/*
 * Suffle 
 */

    int getShuffleSize() {
        return this.mappedKeyValue.size();
    }

	/*
     * 释放initial_map和initial_reduce的内存
	 */

    void initialRelease() {
        this.initialKeyValue = null;

    }

    /*
     * 按key值进行排序
     *
    */
    void cSort() {
        Collections.sort(this.list);
    }

    //Internal Grouping
    void grouping() {
        IntermediateKey tmpKey;
        GroupedKeyValue<IntermediateKey, IntermediateValue> gkv;


        IntermediateKey conKey = this.list.get(0).getKey();
        gkv = new GroupedKeyValue<IntermediateKey, IntermediateValue>(conKey, new GroupedValues<IntermediateValue>(this.list.get(0).getValue()));

        for (int i = 1; i < this.list.size(); i++) {
            tmpKey = this.list.get(i).getKey();

            if (tmpKey.equals(gkv.getKey())) {
                gkv.addValue(this.list.get(i).getValue());
            } else {
                this.gKVList.add(gkv);
                conKey = tmpKey;
                gkv = new GroupedKeyValue<IntermediateKey, IntermediateValue>(conKey, new GroupedValues<IntermediateValue>(this.list.get(i).getValue()));
            }
        }
        this.gKVList.add(gkv);

        this.mappedKeyValue = null;
    }

    void showSuffle() {
        for (GroupedKeyValue<IntermediateKey, IntermediateValue> g_kv : gKVList) {
            System.out.print(g_kv.getKey().toString());
            for (IntermediateValue value : g_kv.getValues()) {
                System.out.print("," + value.toString());
            }
            System.out.println("");
        }
    }

    IntermediateKey getReduceKey(int index) {
        return this.gKVList.get(index).getKey();
    }

    GroupedValues<IntermediateValue> getReduceValues(int index) {
        return this.gKVList.get(index).getValues();
    }

    int getReduceSize() {
        return this.gKVList.size();
    }
}