package com.example.huayeloltool;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.util.MapUtils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class EasyExcelSalesProcessor {

    // 文件夹路径，请根据你的实际情况修改
    private static final String FOLDER_PATH = "C:\\Users\\77646\\Desktop\\苏黄数据";
    private static final String MASTER_FILE_NAME = "苏黄.xlsx";

    /**
     * 主表数据模型
     * 使用 @ExcelProperty 注解将 Java 字段映射到 Excel 列名或列索引
     */
    public static class MasterData {
        @ExcelProperty("姓名") // 映射到 Excel 的 "姓名" 列
        private String name;

        @ExcelProperty("既往最高销量") // 映射到 Excel 的 "既往最高销量" 列
        private Integer previousHighestSales;

        @ExcelProperty("25年1-6月销量") // 映射到 Excel 的 "25年1-6月销量" 列
        private Integer janJunSales;

        // 构造函数，Getter 和 Setter (EasyExcel 读取时需要无参构造函数和Setter)
        public MasterData() {
        }

        public MasterData(String name, Integer previousHighestSales, Integer janJunSales) {
            this.name = name;
            this.previousHighestSales = previousHighestSales;
            this.janJunSales = janJunSales;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getPreviousHighestSales() {
            return previousHighestSales;
        }

        public void setPreviousHighestSales(Integer previousHighestSales) {
            this.previousHighestSales = previousHighestSales;
        }

        public Integer getJanJunSales() {
            return janJunSales;
        }

        public void setJanJunSales(Integer janJunSales) {
            this.janJunSales = janJunSales;
        }

        @Override
        public String toString() {
            return "MasterData{" +
                    "name='" + name + '\'' +
                    ", previousHighestSales=" + previousHighestSales +
                    ", janJunSales=" + janJunSales +
                    '}';
        }
    }

    /**
     * 销售明细数据模型
     */
    public static class SaleRecord {
        @ExcelProperty("姓名")
        private String name;

        @ExcelProperty("日期")
        private Date date; // EasyExcel 可以直接处理 Date 类型

        @ExcelProperty("药品通用名")
        private String medicineName;

        @ExcelProperty("数量")
        private Integer quantity;

        // 构造函数，Getter 和 Setter
        public SaleRecord() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String getMedicineName() {
            return medicineName;
        }

        public void setMedicineName(String medicineName) {
            this.medicineName = medicineName;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        @Override
        public String toString() {
            return "SaleRecord{" +
                    "name='" + name + '\'' +
                    ", date=" + date +
                    ", medicineName='" + medicineName + '\'' +
                    ", quantity=" + quantity +
                    '}';
        }
    }

    /**
     * 主表读取监听器
     * 存储读取到的 MasterData 列表
     */
    public static class MasterDataListener extends AnalysisEventListener<MasterData> {
        private Map<String, MasterData> masterDataMap = MapUtils.newHashMap();

        @Override
        public void invoke(MasterData data, AnalysisContext context) {
            if (data != null && data.getName() != null && !data.getName().trim().isEmpty()) {
                masterDataMap.put(data.getName(), data);
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            System.out.println("主表数据读取完成，共 " + masterDataMap.size() + " 条记录。");
        }

        public Map<String, MasterData> getMasterDataMap() {
            return masterDataMap;
        }
    }

    /**
     * 销售明细读取监听器
     * 存储读取到的 SaleRecord 列表
     */
    public static class SaleRecordListener extends AnalysisEventListener<SaleRecord> {
        private List<SaleRecord> salesRecords = new ArrayList<>();

        @Override
        public void invoke(SaleRecord data, AnalysisContext context) {
            // 简单验证数据的有效性
            if (data != null && data.getName() != null && !data.getName().trim().isEmpty() &&
                    data.getDate() != null && data.getQuantity() != null) {
                salesRecords.add(data);
            } else {
                System.err.println("警告: 读取到不完整的销售记录，已跳过。数据: " + data);
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            System.out.println("销售文件读取完成，共 " + salesRecords.size() + " 条记录。");
        }

        public List<SaleRecord> getSalesRecords() {
            return salesRecords;
        }
    }

    public static void main(String[] args) {
        Path folderPath = Paths.get(FOLDER_PATH);
        Path masterFilePath = folderPath.resolve(MASTER_FILE_NAME);

        try {
            // 1. 读取主表数据
            MasterDataListener masterListener = new MasterDataListener();
            EasyExcel.read(masterFilePath.toFile(), MasterData.class, masterListener).sheet().doRead();
            Map<String, MasterData> masterDataMap = masterListener.getMasterDataMap();
            System.out.println("成功加载主表数据，共 " + masterDataMap.size() + " 位用户。");

            // 2. 读取所有销售明细文件并汇总销售记录
            List<SaleRecord> allSalesRecords = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(folderPath, "*.xlsx")) {
                for (Path filePath : stream) {
                    if (!filePath.getFileName().toString().equals(MASTER_FILE_NAME)) {
                        System.out.println("正在处理销售文件: " + filePath.getFileName());
                        SaleRecordListener salesListener = new SaleRecordListener();
                        EasyExcel.read(filePath.toFile(), SaleRecord.class, salesListener).sheet().doRead();
                        allSalesRecords.addAll(salesListener.getSalesRecords());
                    }
                }
            }
            System.out.println("共读取到 " + allSalesRecords.size() + " 条销售明细记录。");

            // 3. 为每个用户处理销售数据
            for (Map.Entry<String, MasterData> entry : masterDataMap.entrySet()) {
                String userName = entry.getKey();
                MasterData masterDataItem = entry.getValue();

                // 筛选出当前用户的所有销售记录
                List<SaleRecord> userRecords = allSalesRecords.stream()
                        .filter(record -> record.getName() != null && record.getName().equalsIgnoreCase(userName))
                        .collect(Collectors.toList());

                // 计算 "25年1-6月销量" (2025年1月至6月的总销量)
                int totalJanJunSales = userRecords.stream()
                        .filter(record -> {
                            LocalDate recordDate = record.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                            return recordDate.getYear() == 2025 && recordDate.getMonthValue() >= 1 && recordDate.getMonthValue() <= 6;
                        })
                        .mapToInt(SaleRecord::getQuantity)
                        .sum();
                masterDataItem.setJanJunSales(totalJanJunSales);

                // 计算 "既往最高销量" (2025年1月至6月中，月销量最高的月份销量)
                Map<Integer, Integer> monthlySales = userRecords.stream()
                        .filter(record -> {
                            LocalDate recordDate = record.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                            return recordDate.getYear() == 2025 && recordDate.getMonthValue() >= 1 && recordDate.getMonthValue() <= 6;
                        })
                        .collect(Collectors.groupingBy(
                                record -> record.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getMonthValue(), // 按月份分组
                                Collectors.summingInt(SaleRecord::getQuantity) // 统计每月总销量
                        ));

                int previousHighestMonthlySales = monthlySales.values().stream()
                        .max(Comparator.naturalOrder()) // 找出最高月销量
                        .orElse(0); // 如果该用户在1-6月没有销量，则设为0
                masterDataItem.setPreviousHighestSales(previousHighestMonthlySales);
            }
            System.out.println("所有用户销售数据处理完毕。");

            // 4. 更新并保存主表
            // EasyExcel 写入会覆盖原文件，所以这里直接将修改后的 masterDataMap 的值列表写入
            List<MasterData> updatedMasterList = new ArrayList<>(masterDataMap.values());
            EasyExcel.write(masterFilePath.toFile(), MasterData.class)
                    .sheet()
                    .doWrite(updatedMasterList);

            System.out.println("主表 '总表.xlsx' 已成功更新并保存。");

        } catch (IOException e) {
            System.err.println("处理 Excel 文件时发生 I/O 错误: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("发生未知错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}