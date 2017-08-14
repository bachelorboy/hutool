package com.xiaoleilu.hutool.poi.excel;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.xiaoleilu.hutool.collection.IterUtil;
import com.xiaoleilu.hutool.util.BeanUtil;
import com.xiaoleilu.hutool.util.StrUtil;

/**
 * Excel读取器<br>
 * 读取Excel工作簿
 * 
 * @author Looly
 * @since 3.1.0
 */
public class ExcelReader {

	/** 是否忽略空行 */
	private boolean ignoreEmptyRow;
	/** 是否去除单元格元素两边空格 */
	private boolean trimCellValue;
	/** 标题别名 */
	private Map<String, String> headerAlias;
	private Sheet sheet;

	// ------------------------------------------------------------------------------------------------------- Constructor start
	/**
	 * 构造
	 * 
	 * @param bookStream Excel文件的流
	 * @param sheetIndex sheet序号，0表示第一个sheet
	 */
	public ExcelReader(InputStream bookStream, int sheetIndex) {
		this(ExcelUtil.loadBook(bookStream), sheetIndex);
	}

	/**
	 * 构造
	 * 
	 * @param bookStream Excel文件的流
	 * @param sheetName sheet名，第一个默认是sheet1
	 */
	public ExcelReader(InputStream bookStream, String sheetName) {
		this(ExcelUtil.loadBook(bookStream), sheetName);
	}

	/**
	 * 构造
	 * 
	 * @param book {@link Workbook} 表示一个Excel文件
	 * @param sheetIndex sheet序号，0表示第一个sheet
	 */
	public ExcelReader(Workbook book, int sheetIndex) {
		this(book.getSheetAt(sheetIndex));
	}

	/**
	 * 构造
	 * 
	 * @param book {@link Workbook} 表示一个Excel文件
	 * @param sheetName sheet名，第一个默认是sheet1
	 */
	public ExcelReader(Workbook book, String sheetName) {
		this(book.getSheet(sheetName));
	}

	/**
	 * 构造
	 * 
	 * @param sheet Excel中的sheet
	 */
	public ExcelReader(Sheet sheet) {
		this.sheet = sheet;
	}
	// ------------------------------------------------------------------------------------------------------- Constructor end

	// ------------------------------------------------------------------------------------------------------- Getters and Setters start
	/**
	 * 是否忽略空行
	 * 
	 * @return 是否忽略空行
	 */
	public boolean isIgnoreEmptyRow() {
		return ignoreEmptyRow;
	}

	/**
	 * 设置是否忽略空行
	 * 
	 * @param ignoreEmptyRow 是否忽略空行
	 * @return this
	 */
	public ExcelReader setIgnoreEmptyRow(boolean ignoreEmptyRow) {
		this.ignoreEmptyRow = ignoreEmptyRow;
		return this;
	}

	/**
	 * 是否去掉单元格值两边空格
	 * 
	 * @return 是否去掉单元格值两边空格
	 */
	public boolean isTrimCellValue() {
		return trimCellValue;
	}

	/**
	 * 设置是否去掉单元格值两边空格
	 * 
	 * @param trimCellValue 是否去掉单元格值两边空格
	 * @return this
	 */
	public ExcelReader setTrimCellValue(boolean trimCellValue) {
		this.trimCellValue = trimCellValue;
		return this;
	}
	// ------------------------------------------------------------------------------------------------------- Getters and Setters end

	/**
	 * 读取工作簿中指定的Sheet的所有行列数据
	 * 
	 * @return 行的集合，一行使用List表示
	 */
	public List<List<Object>> read() {
		return read(0, Integer.MAX_VALUE);
	}

	/**
	 * 读取工作簿中指定的Sheet
	 * 
	 * @param startRowIndex 起始行（包含）
	 * @param endRowIndex 结束行（包含）
	 * @return 行的集合，一行使用List表示
	 */
	public List<List<Object>> read(int startRowIndex, int endRowIndex) {
		List<List<Object>> resultList = new ArrayList<>();

		startRowIndex = Math.max(startRowIndex, sheet.getFirstRowNum());// 读取起始行（包含）
		endRowIndex = Math.min(endRowIndex, sheet.getLastRowNum());// 读取结束行（包含）
		List<Object> rowList;
		for (int i = startRowIndex; i <= endRowIndex; i++) {
			rowList = readRow(sheet.getRow(i));
			if (false == rowList.isEmpty() || false == ignoreEmptyRow) {
				resultList.add(rowList);
			}
		}
		return resultList;
	}

	/**
	 * 读取Excel为Map的列表，读取所有行，默认第一行做为标题，数据从第二行开始<br>
	 * Map表示一行，标题为key，单元格内容为value
	 * 
	 * @return Map的列表
	 */
	public List<Map<String, Object>> readAll() {
		return read(0, 1, Integer.MAX_VALUE);
	}

	/**
	 * 读取Excel为Map的列表<br>
	 * Map表示一行，标题为key，单元格内容为value
	 * 
	 * @param headerRowIndex 标题所在行，如果标题行在读取的内容行中间，这行做为数据将忽略
	 * @param startRowIndex 起始行（包含）
	 * @param endRowIndex 读取结束行（包含）
	 * @return Map的列表
	 */
	public List<Map<String, Object>> read(int headerRowIndex, int startRowIndex, int endRowIndex) {
		// 边界判断
		final int firstRowNum = sheet.getFirstRowNum();
		final int lastRowNum = sheet.getLastRowNum();
		if (headerRowIndex < firstRowNum) {
			throw new IndexOutOfBoundsException(StrUtil.format("Header row index {} is lower than first row index {}.", headerRowIndex, firstRowNum));
		} else if (headerRowIndex > lastRowNum) {
			throw new IndexOutOfBoundsException(StrUtil.format("Header row index {} is greater than last row index {}.", headerRowIndex, firstRowNum));
		}
		startRowIndex = Math.max(startRowIndex, firstRowNum);// 读取起始行（包含）
		endRowIndex = Math.min(endRowIndex, lastRowNum);// 读取结束行（包含）

		// 读取header
		List<Object> headerList = readRow(sheet.getRow(headerRowIndex));

		final List<Map<String, Object>> result = new ArrayList<>(endRowIndex - startRowIndex + 1);
		List<Object> rowList;
		for (int i = startRowIndex; i <= endRowIndex; i++) {
			if (i != headerRowIndex) {
				// 跳过标题行
				rowList = readRow(sheet.getRow(i));
				if (false == rowList.isEmpty() || false == ignoreEmptyRow) {
					result.add(IterUtil.toMap(aliasHeader(headerList), rowList));
				}
			}
		}
		return result;
	}

	/**
	 * 读取Excel为Bean的列表，读取所有行，默认第一行做为标题，数据从第二行开始
	 * 
	 * @param <T> Bean类型
	 * @param beanType 每行对应Bean的类型
	 * @return Map的列表
	 */
	public <T> List<T> readAll(Class<T> beanType) {
		return read(0, 1, Integer.MAX_VALUE, beanType);
	}

	/**
	 * 读取Excel为Bean的列表
	 * 
	 * @param <T> Bean类型
	 * @param headerRowIndex 标题所在行，如果标题行在读取的内容行中间，这行做为数据将忽略
	 * @param startRowIndex 起始行（包含）
	 * @param endRowIndex 读取结束行（包含）
	 * @param beanType 每行对应Bean的类型
	 * @return Map的列表
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> read(int headerRowIndex, int startRowIndex, int endRowIndex, Class<T> beanType) {
		final List<Map<String, Object>> mapList = read(headerRowIndex, startRowIndex, endRowIndex);
		if (Map.class.isAssignableFrom(beanType)) {
			return (List<T>) mapList;
		}

		final List<T> beanList = new ArrayList<>(mapList.size());
		for (Map<String, Object> map : mapList) {
			beanList.add(BeanUtil.mapToBean(map, beanType, false));
		}
		return beanList;
	}

	// ------------------------------------------------------------------------------------------------------- Private methods start
	/**
	 * 读取一行
	 * 
	 * @param row 行
	 * @return 单元格值列表
	 */
	private List<Object> readRow(Row row) {
		final List<Object> cellValues = new ArrayList<>();
		if (null != row) {
			final Iterator<Cell> celIter = row.cellIterator();
			Cell cell;
			while (celIter.hasNext()) {
				cell = celIter.next();
				cellValues.add(ExcelUtil.getCellValue(cell, this.trimCellValue));
			}
		}
		return cellValues;
	}

	/**
	 * 转换标题别名，如果没有别名则使用原标题
	 * 
	 * @param headerList 原标题列表
	 * @return 转换别名列表
	 */
	private List<String> aliasHeader(List<Object> headerList) {
		final ArrayList<String> result = new ArrayList<>();
		String header;
		String alias;
		for (Object headerObj : headerList) {
			header = headerObj.toString();
			alias = this.headerAlias.get(header);
			if (null == alias) {
				alias = header;
			}
			result.add(alias);
		}
		return result;
	}
	// ------------------------------------------------------------------------------------------------------- Private methods end
}