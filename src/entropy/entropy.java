package entropy;

import java.awt.EventQueue;
import java.awt.Frame;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import weka.attributeSelection.CorrelationAttributeEval;
import weka.attributeSelection.Ranker;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JTable;
import javax.swing.RowSorter;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

public class entropy extends JFrame {

	public JPanel contentPane;
	private JTextField tfDataFilePath;
	private static JTable table;
	private DefaultTableModel model;
	private static int numberOfRows = 0;
	private static int numberOfColumns = 0;
	private List<Record> recordList;
	RowSorter<TableModel> sorter;
	File dataFile;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					entropy frame = new entropy();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	// Method for sorting
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		Map<K, V> result = new LinkedHashMap<>();
		Stream<Entry<K, V>> st = map.entrySet().stream();

		st.sorted(Comparator.comparing(e -> e.getValue())).forEach(e -> result.put(e.getKey(), e.getValue()));

		return result;
	}

	/**
	 * uses the filter for feature selection -- Weka method using
	 * CorrelationAttributeEval evaluator and Ranker Search - Task 1
	 */
	public static void CorrelationCoeffAttributeMethod(int columns, Instances data, String filename) throws Exception {

		PrintWriter pw = new PrintWriter(filename, "UTF-8");
		weka.filters.supervised.attribute.AttributeSelection filter = new weka.filters.supervised.attribute.AttributeSelection();
		CorrelationAttributeEval eval = new CorrelationAttributeEval();
		Ranker search = new Ranker();
		// set numToSelect to inout provided by use i.e. K
		search.setNumToSelect(columns);
		filter.setEvaluator(eval);
		filter.setSearch(search);
		filter.setInputFormat(data);
		// Apply filter to get selected attributes
		Instances newData = Filter.useFilter(data, filter);
		// print output in TOPKFetaures1.txt file
		pw.println("Selected Top Fetaures using WEKA:\t" + (newData.numAttributes() - 1));

		for (int j = 0; j < newData.numAttributes(); j++) {
			String geneName = newData.attribute(j).name();
			if (geneName.contains("att"))
				geneName = geneName.replace("att", "g");
			if ((!newData.attribute(j).isNominal()))
				pw.println("\n\t\t" + geneName);

		}
		pw.close();
	}

	/**
	 * Method for calculating entropy based information gain genes , discretized
	 * and select top K features - Task 2
	 * 
	 * @param columns
	 * @param fileName
	 * @param isForDiscretized
	 * @param isForCorrelationCoeff
	 */
	public static void OrderGenes(int columns, String fileName, int inputKValue, Boolean isForDiscretized,
			Boolean isForCorrelationCoeff) {
		columns++;
		DefaultTableModel dtm = (DefaultTableModel) table.getModel();
		Map<String, String> mappedRecords = new HashMap<String, String>();
		Map<String, Double> gainRecords = new HashMap<String, Double>();
		Map<String, Double> sortedGainRecords = new HashMap<String, Double>();
		Map<String, List<Double>> TopFeatures1data = new HashMap<String, List<Double>>();
		Map<String, List<Double>> TopFeatures2data = new HashMap<String, List<Double>>();

		for (int i = 0; i < columns - 1; i++) {
			List<String> tuples = new ArrayList<String>();
			List<String> columnSplitRecords = new ArrayList<String>();
			List<Double> intraColumnSplitGains = new ArrayList<Double>();
			int pClassCount = 0;
			int nClassCount = 0;
			Integer[] S1 = new Integer[2];
			Integer[] S2 = new Integer[2];
			Integer[] S3 = new Integer[2];
			double entropyOfS = 0;
			for (int j = 0; j < numberOfRows; j++) {
				String tuple = "(" + dtm.getValueAt(j, i).toString() + ","
						+ dtm.getValueAt(j, numberOfColumns - 1).toString() + ")";
				tuples.add(tuple);
			}

			String[] array = tuples.toArray(new String[tuples.size()]);
			String temp;
			for (int j = 0; j < array.length - 1; j++) {
				for (int k = 1; k < array.length - j; k++) {
					String key = array[k];
					double currentValue = Double.parseDouble(key.subSequence(1, key.indexOf(',')).toString());
					String nextKey = array[k - 1];
					double nextValue = Double.parseDouble(nextKey.subSequence(1, nextKey.indexOf(',')).toString());
					if (nextValue > currentValue) {
						temp = nextKey;
						array[k - 1] = key;
						array[k] = temp;
					}
				}
			}
			tuples.clear();
			tuples = Arrays.asList(array);

			// take count of number of positive and negative samples in complete
			// dataset S
			for (int t = 1; t <= tuples.size() - 1; t++) {
				if (tuples.get(t).contains("positive"))
					pClassCount++;
				else if (tuples.get(t).contains("negative"))
					nClassCount++;
			}
			// Calculate overall Entropy of set S
			entropyOfS = (((-pClassCount
					* (Math.log(pClassCount) / Math.log(2) - Math.log(pClassCount + nClassCount) / Math.log(2)))
					/ (pClassCount + nClassCount))
					- (nClassCount
							* (Math.log(nClassCount) / Math.log(2) - Math.log(pClassCount + nClassCount) / Math.log(2)))
							/ (pClassCount + nClassCount));

			pClassCount = 0;
			nClassCount = 0;
			// divide the given dataset into 3 bins and calculate entropy
			for (int a = 1; a <= tuples.size() - 1; a++) {
				List<String> bin1 = tuples.subList(0, a);
				for (int b = a + 1; b <= tuples.size() - 1; b++) {
					List<String> bin2 = tuples.subList(a, b);
					List<String> bin3 = tuples.subList(b, tuples.size());

					int commaIndex = bin1.get(bin1.size() - 1).indexOf(',');
					double preSplitValue = Double
							.parseDouble(bin1.get(bin1.size() - 1).subSequence(1, commaIndex).toString());
					commaIndex = bin2.get(0).indexOf(',');
					double postSplitValue = Double.parseDouble(bin2.get(0).subSequence(1, commaIndex).toString());
					commaIndex = bin3.get(0).indexOf(',');
					double SplitValue3 = Double.parseDouble(bin3.get(0).subSequence(1, commaIndex).toString());

					// get lower bound and upper bound for bin1
					double bin1LB = Double.NEGATIVE_INFINITY;
					double bin1UB = (preSplitValue + postSplitValue) / 2;

					// get the count of positive and negative samples in bin 1
					for (int c = 0; c < bin1.size(); c++) {
						if (bin1.get(c).contains("positive"))
							pClassCount++;
						else if (bin1.get(c).contains("negative"))
							nClassCount++;

					}

					S1[0] = pClassCount;
					S1[1] = nClassCount;
					pClassCount = 0;
					nClassCount = 0;
					// get lower bound and upper bound for bin1
					double bin2LB = (preSplitValue + postSplitValue) / 2;
					double bin2UB = (postSplitValue + SplitValue3) / 2;

					// get the count of positive and negative samples in bin 2
					for (int d = 0; d < bin2.size(); d++) {
						if (bin2.get(d).contains("positive"))
							pClassCount++;
						else if (bin2.get(d).contains("negative"))
							nClassCount++;
					}
					S2[0] = pClassCount;
					S2[1] = nClassCount;
					pClassCount = 0;
					nClassCount = 0;

					// get lower bound and upper bound for bin1
					double bin3LB = (postSplitValue + SplitValue3) / 2;
					double bin3UB = Double.POSITIVE_INFINITY;

					// get the count of positive and negative samples in bin 3
					for (int e = 0; e < bin3.size(); e++) {
						if (bin3.get(e).contains("positive"))
							pClassCount++;
						else if (bin3.get(e).contains("negative"))
							nClassCount++;
					}
					S3[0] = pClassCount;
					S3[1] = nClassCount;
					pClassCount = 0;
					nClassCount = 0;
					// calculate entropy of S1, S2 and S3
					double entropyOfS1 = (((-S1[0]
							* (Math.log(S1[0]) / Math.log(2) - Math.log(S1[0] + S1[1]) / Math.log(2)))
							/ (S1[0] + S1[1]))
							- (S1[1] * (Math.log(S1[1]) / Math.log(2) - Math.log(S1[0] + S1[1]) / Math.log(2)))
									/ (S1[0] + S1[1]));

					if (Double.isNaN(entropyOfS1))
						entropyOfS1 = 0;

					double entropyOfS2 = (((-S2[0]
							* (Math.log(S2[0]) / Math.log(2) - Math.log(S2[0] + S2[1]) / Math.log(2)))
							/ (S2[0] + S2[1]))
							- (S2[1] * (Math.log(S2[1]) / Math.log(2) - Math.log(S2[0] + S2[1]) / Math.log(2)))
									/ (S2[0] + S2[1]));

					if (Double.isNaN(entropyOfS2))
						entropyOfS2 = 0;

					double entropyOfS3 = (((-S3[0]
							* (Math.log(S3[0]) / Math.log(2) - Math.log(S3[0] + S3[1]) / Math.log(2)))
							/ (S3[0] + S3[1]))
							- (S3[1] * (Math.log(S3[1]) / Math.log(2) - Math.log(S3[0] + S3[1]) / Math.log(2)))
									/ (S3[0] + S3[1]));

					if (Double.isNaN(entropyOfS3))
						entropyOfS3 = 0;

					/*
					 * double iSOfS1S2 =
					 * (((S1[0]+S1[1])*entropyOfS1)/tuples.size() +
					 * ((S2[0]+S2[1])*entropyOfS2)/tuples.size());
					 * if(Double.isNaN(iSOfS1S2)) iSOfS1S2 = 0;
					 */
					double iSOfS1S2S3 = (((S1[0] + S1[1]) * entropyOfS1) / tuples.size()
							+ ((S2[0] + S2[1]) * entropyOfS2) / tuples.size())
							+ ((S3[0] + S3[1]) * entropyOfS3) / tuples.size();
					if (Double.isNaN(iSOfS1S2S3))
						iSOfS1S2S3 = 0;

					// calculate gain for current dataset
					double gain = entropyOfS - iSOfS1S2S3;
					DecimalFormat twoDForm = new DecimalFormat("#.###");

					String columnSplitRecord = "" + twoDForm.format(gain) + ";" + bin1LB + ";" + twoDForm.format(bin1UB)
							+ ";" + S1[0] + ";" + S1[1] + ";" + twoDForm.format(bin2LB) + ";" + twoDForm.format(bin2UB)
							+ ";" + S2[0] + ";" + S2[1] + ";" + twoDForm.format(bin3LB) + ";" + bin3UB + ";" + S3[0]
							+ ";" + S3[1];
					columnSplitRecords.add(columnSplitRecord);
					if (Double.isNaN(gain))
						gain = 0;
					intraColumnSplitGains.add(gain);
				}
			}
			int maxGainIndex = intraColumnSplitGains.indexOf(Collections.max(intraColumnSplitGains));
			String maxGainRecord = columnSplitRecords.get(maxGainIndex);
			gainRecords.put(dtm.getColumnName(i), intraColumnSplitGains.get(maxGainIndex));
			mappedRecords.put(dtm.getColumnName(i), maxGainRecord);
		}
		// sorting the records based on maxgain and storing into sortedlist
		sortedGainRecords = sortByValue(gainRecords);

		try {
			PrintWriter pw = new PrintWriter(fileName, "UTF-8");
			Object[] keys = sortedGainRecords.keySet().toArray();
			List<String> genesCollection = new ArrayList<String>();
			List<List<String>> intervalLists = new ArrayList<List<String>>();
			List<String> keyList = new ArrayList<String>();

			for (int p = sortedGainRecords.size() - 1; p >= sortedGainRecords.size() - inputKValue; p--) {
				keyList.add(keys[p].toString() + "");
			}
			// Discretize data
			if (isForDiscretized) {

				pw.println(keyList);
				for (int i = sortedGainRecords.size() - 1; i >= sortedGainRecords.size() - inputKValue; i--) {

					List<String> intervalDistribution = new ArrayList<String>();
					for (int j = 0; j < numberOfRows; j++) {
						String record = mappedRecords.get(keys[i]);
						genesCollection.add(keys[i].toString());
						String[] records = record.split(";");
						String tuple = dtm.getValueAt(j, i).toString();

						if (Double.parseDouble(tuple) >= Double.parseDouble(records[1])
								&& Double.parseDouble(tuple) < Double.parseDouble(records[2])) {
							intervalDistribution.add("a");
						} else if (Double.parseDouble(tuple) >= Double.parseDouble(records[5])
								&& Double.parseDouble(tuple) < Double.parseDouble(records[6])) {
							intervalDistribution.add("b");
						} else
							intervalDistribution.add("c");

					}

					intervalLists.add(intervalDistribution);

				}
				for (int i = 0; i < numberOfRows; i++) {
					pw.print("[ ");
					for (List<String> intrvls : intervalLists) {

						pw.print(intrvls.get(i) + " , ");

					}
					String classTuple = dtm.getValueAt(i, numberOfColumns - 1).toString();
					// Print the output of discretize data to Entropydata.txt
					// file
					pw.print(classTuple + " ]");
					pw.println();
				}
			}
			// Calculate correlation coefficients for selected top features by
			// Task 1 and Task 2
			else {
				if (isForCorrelationCoeff) {
					try {
						List<String> topGenes = new ArrayList<String>();
						List<Integer> columnNoofgenes = new ArrayList<Integer>();

						/*
						 * Read topKFeatures1 file generated using weka feature
						 * selection method
						 */
						BufferedReader reader = new BufferedReader(new FileReader("TopKFetaures1.txt"));
						String str = null;
						while ((str = reader.readLine()) != null) {
							if (!str.isEmpty())
								topGenes.add(str);
						}
						reader.close();
						for (int i = 0; i < topGenes.size(); i++) {
							int columnNo = dtm.findColumn(topGenes.get(i).toString().trim());
							if (columnNo != -1)
								columnNoofgenes.add(columnNo);
						}
						for (int i = 0; i < columnNoofgenes.size(); i++) {
							List<Double> tuples = new ArrayList<Double>();
							for (int j = 0; j < numberOfRows; j++) {
								double tuple = Double.parseDouble(dtm.getValueAt(j, columnNoofgenes.get(i)).toString());
								tuples.add(tuple);
							}
							TopFeatures1data.put(dtm.getColumnName(columnNoofgenes.get(i)), tuples);
						}
						for (int i = sortedGainRecords.size() - 1; i >= sortedGainRecords.size() - inputKValue; i--) {
							List<Double> tuples = new ArrayList<Double>();
							int columnnumber = dtm.findColumn(keys[i].toString());
							for (int j = 0; j < numberOfRows; j++) {
								double tuple = Double.parseDouble(dtm.getValueAt(j, columnnumber).toString());
								tuples.add(tuple);
							}
							TopFeatures2data.put(keys[i].toString(), tuples);
						}
						// Declare list to store coefficient reocrds for
						// respective gene pairs.
						Map<String, Double> coefficientRecords = new HashMap<String, Double>();
						Object[] topKFeauture1Keys = TopFeatures1data.keySet().toArray();
						Object[] topKFeauture2Keys = TopFeatures2data.keySet().toArray();

						for (int i = 0; i < TopFeatures1data.size(); i++) {
							List<Double> geneValues = TopFeatures1data.get(topKFeauture1Keys[i]);
							Double sum = 0.0;
							// Calculate Mean for TopKFeatures1 gene
							for (int w = 0; w < geneValues.size(); w++) {
								sum += geneValues.get(w);
							}
							Double topf1Mean = sum / geneValues.size();
							double temp = 0;
							// Calculate Std deviation for TopKFeatures1 gene
							for (double a : geneValues)
								temp += (topf1Mean - a) * (topf1Mean - a);
							Double stdDeviation = Math.sqrt(temp / geneValues.size());
							for (int j = 0; j < TopFeatures2data.size(); j++) {
								List<Double> geneValues1 = TopFeatures2data.get(topKFeauture2Keys[j]);
								Double sum1 = 0.0;
								// Calculate Mean for TopKFeatures2 gene
								for (int w = 0; w < geneValues1.size(); w++) {
									sum1 += geneValues1.get(w);
								}
								Double topf2Mean = sum1 / geneValues1.size();
								double temp1 = 0;
								for (double a : geneValues1)
									temp1 += (topf2Mean - a) * (topf2Mean - a);
								// Calculate Std deviation for TopKFeatures2
								// gene
								Double stdDeviation1 = Math.sqrt(temp1 / geneValues1.size());

								double summation = 0;
								for (int l = 0; l < geneValues.size(); l++)
									summation += (geneValues.get(l) * geneValues1.get(l));
								/* Calculate Coefficient value for current K2
								 pair */
								double coefficient = (summation - (geneValues.size() * topf1Mean * topf2Mean))
										/ (geneValues.size() * stdDeviation * stdDeviation1);
								/* Store the k2 pairs and respective coefficient
								 value in dictionary. */
								coefficientRecords.put("[" + topKFeauture1Keys[i] + " " + topKFeauture2Keys[j] + "]",
										coefficient);

							}
						}
						/* Sort all coefficient values and store it in sortedcoefficientRecords list */
						Map<String, Double> sortedCoefficientRecords = sortByValue(coefficientRecords);
						Object[] sortedKeys = sortedCoefficientRecords.keySet().toArray();
						Object[] sortedCoefficientValue = sortedCoefficientRecords.values().toArray();
						DecimalFormat twoDForm = new DecimalFormat("#.###");
						pw.println("K2 Pairs : Correlation Coefficient");
						pw.println();
						for (int i = sortedCoefficientRecords.size() - 1; i >= 0; i--) {
							pw.println(sortedKeys[i] + " : " + twoDForm.format(sortedCoefficientValue[i]));
						}

					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
			/* Print selected top features in TopKFeatures2.txt file and Entropbin.txt file */
			if (!isForDiscretized) {
				PrintWriter pw1 = new PrintWriter("TopKFetaures2.txt", "UTF-8");
				pw1.println("Selected Top Features using Entropy Based Method:");
				for (int i = sortedGainRecords.size() - 1; i >= sortedGainRecords.size() - inputKValue; i--) {
					String record = mappedRecords.get(keys[i]);
					String[] records = record.split(";");
					if (!isForCorrelationCoeff) {
						pw.println(keys[i] + "  Info Gain : " + records[0] + " Bins : (" + records[1] + " , "
								+ records[2] + ") , " + records[3] + " , " + records[4] + " ; [" + records[5] + " , "
								+ records[6] + ") , " + records[7] + " , " + records[8] + " ; [" + records[9] + " , "
								+ records[10] + ") , " + records[11] + "," + records[12]);
					}
					pw1.println("\t\t" + keys[i]);
				}
				pw1.close();
			}
			pw.close();

		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
/*
 * Convert given input text file to CSV file
 * */
	
	public static void ConvertTextToCSV(String fileName, String filePath, File dataFile) {
		try {
			BufferedReader bReader = new BufferedReader(new FileReader(dataFile));
			PrintWriter out = new PrintWriter(new BufferedWriter(
					new FileWriter(filePath + "//" + fileName.substring(0, fileName.lastIndexOf(".")) + ".csv")));
			String str = null;
			while ((str = bReader.readLine()) != null) {
				out.println(str);
			}

			out.close();
			bReader.close();
		} catch (Exception ex) {
		}
	}
	 /* Convert CSV file to arff file format for WEKA method
	 * */
	public static void ConvertCSVtoARFF(String fileName, String filePath) {
		try {
			CSVLoader loader = new CSVLoader();
			loader.setSource(new File(filePath + "//" + fileName.substring(0, fileName.lastIndexOf(".")) + ".csv"));
			loader.setOptions(new String[] { "-H" });
			Instances data = loader.getDataSet();

			// save ARFF
			ArffSaver saver = new ArffSaver();
			saver.setInstances(data);
			saver.setFile(new File(filePath + "//" + fileName.substring(0, fileName.lastIndexOf(".")) + ".arff"));
			saver.writeBatch();
		} catch (Exception ex) {
		}
	}

	/*
	 * Get input file and call Weka method for TopKFeatures1.txt
	 * */
	public void getFileDetails(int Columns) {

		try {
			String filePath = dataFile.getParent();
			String filename = dataFile.getName();
			String extension = filename.substring(filename.lastIndexOf(".") + 1, filename.length());
			if (extension.equals("txt"))
				ConvertTextToCSV(filename, filePath, dataFile);

			ConvertCSVtoARFF(filename, filePath);

			DataSource source = new DataSource(
					filePath + "//" + filename.substring(0, filename.lastIndexOf(".")) + ".arff");
			Instances data = source.getDataSet();
			if (data.classIndex() == -1)
				data.setClassIndex(data.numAttributes() - 1);

			CorrelationCoeffAttributeMethod(Columns, data, "TopKFetaures1.txt");
		} catch (Exception ex) {
		}
	}

	/**
	 * Create the frame.
	 */
	public entropy() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(250, 150, 767, 500);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JButton btnBrowse = new JButton("Browse");
		btnBrowse.setBounds(435, 16, 89, 23);
		contentPane.add(btnBrowse);

		JLabel lblDataFile = new JLabel("Data File");
		lblDataFile.setBounds(40, 20, 61, 14);
		contentPane.add(lblDataFile);

		tfDataFilePath = new JTextField();
		tfDataFilePath.setBounds(111, 17, 314, 20);
		contentPane.add(tfDataFilePath);
		tfDataFilePath.setColumns(10);

		table = new JTable();
		JScrollPane scrollPane = new JScrollPane(table);
		table.setFillsViewportHeight(true);
		scrollPane.setBounds(40, 52, 484, 346);
		contentPane.add(scrollPane);

		/*button to generate Entropybin.txt and TopKFeatures2.txt*/
		JButton btnDataEntropy = new JButton("Data Entropy Method");
		btnDataEntropy.setEnabled(false);
		btnDataEntropy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {

					String inputNumberOfColumns = (String) JOptionPane.showInputDialog(new Frame(),
							"Enter number of columns", "User input", JOptionPane.PLAIN_MESSAGE, null, null, null);

					if (inputNumberOfColumns == "")
						return;

					int inputOfColumns = Integer.parseInt(inputNumberOfColumns);
					if (inputOfColumns > numberOfColumns - 1) {
						JOptionPane.showMessageDialog(null, "Input given is more than the records available", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					OrderGenes(numberOfColumns - 1, "Entropybins.txt", inputOfColumns, false, false);
					JOptionPane.showMessageDialog(null,
							"Given number of Genes are ordered. Check Entropybins.txt and TopKFeatures2.txt", "Message",
							JOptionPane.INFORMATION_MESSAGE);
				} catch (Exception e) {
					
					e.printStackTrace();
				}
			}
		});
		btnDataEntropy.setBounds(538, 110, 187, 23);
		contentPane.add(btnDataEntropy);

		/*button to generate Entropydata.txt*/
		JButton btnDiscretizeData = new JButton("Discretize Data");
		btnDiscretizeData.setEnabled(false);
		btnDiscretizeData.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {

					String inputNumberOfColumns = (String) JOptionPane.showInputDialog(new Frame(),
							"Enter number of columns", "User input", JOptionPane.PLAIN_MESSAGE, null, null, null);

					if (inputNumberOfColumns == "")
						return;

					int inputOfColumns = Integer.parseInt(inputNumberOfColumns);
					if (inputOfColumns > numberOfColumns - 1) {
						JOptionPane.showMessageDialog(null, "Input given is more than the records available", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					OrderGenes(numberOfColumns - 1, "EntropyData.txt", inputOfColumns, true, false);
					JOptionPane.showMessageDialog(null, "Given number of Genes are discretized. Check EntropyData.txt",
							"Message", JOptionPane.INFORMATION_MESSAGE);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		btnDiscretizeData.setBounds(538, 150, 187, 23);
		contentPane.add(btnDiscretizeData);

		/*  Select top K features using WEKA */
		JButton btnTopFetauresWeka = new JButton("Select Top Features- WEKA");
		btnTopFetauresWeka.setHorizontalAlignment(SwingConstants.LEFT);
		btnTopFetauresWeka.setEnabled(false);
		btnTopFetauresWeka.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					String inputNumberOfColumns = (String) JOptionPane.showInputDialog(new Frame(),
							"Enter number of columns", "User input", JOptionPane.PLAIN_MESSAGE, null, null, null);

					if (inputNumberOfColumns == "")
						return;

					int inputOfColumns = Integer.parseInt(inputNumberOfColumns);
					if (inputOfColumns > numberOfColumns - 1) {
						JOptionPane.showMessageDialog(null, "Input given is more than the records available", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}

					getFileDetails(inputOfColumns);
					JOptionPane.showMessageDialog(null, "Features Selection Successful. Please check TopKFeatures1.txt",
							"Message", JOptionPane.INFORMATION_MESSAGE);

				} catch (Exception ex) {

				}
			}

		});
		btnTopFetauresWeka.setBounds(538, 70, 187, 23);
		contentPane.add(btnTopFetauresWeka);

		/* button to generate Correlationgenesdata.txt*/
		JButton btnCorrCoeff = new JButton("Correlation Coefficients");
		btnCorrCoeff.setEnabled(false);
		btnCorrCoeff.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String inputNumberOfColumns = (String) JOptionPane.showInputDialog(new Frame(),
						"Enter number of columns", "User input", JOptionPane.PLAIN_MESSAGE, null, null, null);

				if (inputNumberOfColumns == "")
					return;

				int inputOfColumns = Integer.parseInt(inputNumberOfColumns);
				if (inputOfColumns > numberOfColumns - 1) {
					JOptionPane.showMessageDialog(null, "Input given is more than the records available", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				getFileDetails(inputOfColumns);
				OrderGenes(numberOfColumns - 1, "Correlationgenesdata.txt", inputOfColumns, false, true);
				JOptionPane.showMessageDialog(null, "Please check Correlationgenesdata.txt", "Message",
						JOptionPane.INFORMATION_MESSAGE);
			}
		});
		btnCorrCoeff.setBounds(538, 190, 187, 23);
		contentPane.add(btnCorrCoeff);

		btnBrowse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				JFileChooser chooser = new JFileChooser();
				chooser.setCurrentDirectory(new java.io.File("."));
				chooser.setDialogTitle("Select the file");
				chooser.addChoosableFileFilter(new FileNameExtensionFilter("Text File", "txt"));
				chooser.addChoosableFileFilter(new FileNameExtensionFilter("CSV", "csv"));
				chooser.setAcceptAllFileFilterUsed(true);
				chooser.setFileSelectionMode(JFileChooser.OPEN_DIALOG);
				chooser.showOpenDialog(null);
				dataFile = chooser.getSelectedFile();
				if (!dataFile.exists()) {
					JOptionPane.showMessageDialog(null, "File does not exist. Please check again", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (dataFile != null) {

					numberOfColumns = 0;
					numberOfRows = 0;
					tfDataFilePath.setText(dataFile.getAbsolutePath());
					tfDataFilePath.setEnabled(false);
					ArrayList<String> columnNames = new ArrayList<String>();
					try {
						BufferedReader reader = new BufferedReader(new FileReader((String) dataFile.getAbsolutePath()));
						String line = null;
						recordList = new ArrayList<Record>();
						// recordList.clear();
						while ((line = reader.readLine()) != null) {
							String[] values = line.split(",");
							Record recordObj = null;
							for (String str : values) {
								numberOfColumns++;
								if (recordObj == null) {
									recordObj = new Record();
									recordObj.GeneList = new ArrayList<String>();
								}
								if (str.equals("positive") || str.equals("negative")) {

									numberOfRows++;
									recordObj.Class = str;
									recordObj.GeneList.add(str);
									recordList.add(recordObj);
									recordObj = null;
								} else {
									recordObj.GeneList.add(str);
								}
							}
						}
						reader.close();
						numberOfColumns = numberOfColumns / numberOfRows;
						for (int i = 1; i <= numberOfColumns - 1; i++) {
							columnNames.add("g" + i);
						}
						columnNames.add("Class");
						String[] columns = columnNames.toArray(new String[columnNames.size()]);
						table.setModel(new DefaultTableModel(columns, 0));
						table.setAutoResizeMode(0);
						model = (DefaultTableModel) table.getModel();
						String[] columnData;
						for (Record recordObj : recordList) {
							columnData = recordObj.GeneList.toArray(new String[recordObj.GeneList.size()]);
							model.addRow(columnData);
						}
						model.fireTableRowsInserted(0, numberOfRows);
						sorter = new TableRowSorter<TableModel>(model);
						table.setRowSorter(sorter);
						btnTopFetauresWeka.setEnabled(true);
						btnDataEntropy.setEnabled(true);
						btnDiscretizeData.setEnabled(true);
						btnCorrCoeff.setEnabled(true);

					} catch (Exception e1) {
						JOptionPane.showMessageDialog(null, "File not valid.", "Error", JOptionPane.ERROR_MESSAGE);					
						e1.printStackTrace();
						return;
					}
				}

			}
		});

	}
}
