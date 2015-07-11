package com.josedeveloper

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

case class Padron(codDistrito: String, descDistrito: String, codDistBarrio: String,
             descBarrio: String, codBarrio: String, codDistSeccion: String, spainMen: Integer,
             spainWomen: Integer, foreignerMen: Integer, foreignerWomen: Integer) {

  def numberOfPeople() : Int = {
    return spainMen + spainWomen + foreignerMen + foreignerWomen;
  }

}

object ScalaApp extends App {

  val sc = new SparkContext("local", "Simple Application", "$SPARK_HOME"
    , List("target/SimpleApp-0.1.jar"))

  val file = sc.textFile("Rango_Edades_Seccion_201506.csv")

  val data = file.map(line => line.split(";").map(_.trim))
      //the csv header is excluded
      .mapPartitionsWithIndex((idx, iter) => if (idx == 0) iter.drop(1) else iter).persist()

  //Map to Padron objects
  val dataMapped = data.map(line => (line(1), new Padron(line(0), line(1),
    line(2), line(3), line(4), line(5),
    getIntValue(line(6)),
    getIntValue(line(7)),
    getIntValue(line(8)),
    getIntValue(line(9)))))

  //Groupped by district
  val grouppedData = dataMapped.reduceByKey((x: Padron, y: Padron) => new Padron(x.codDistrito, x.descDistrito,
    x.codDistBarrio, x.descBarrio, x.codBarrio, x.codDistSeccion, x.spainMen + y.spainMen,
    x.spainWomen + y.spainWomen, x.foreignerMen + y.foreignerMen,
    x.foreignerWomen + y.foreignerWomen))

  //sorted list total spanish men by district
  grouppedData.collect().sortBy(_._2.spainMen)
    .foreach(x => println(x._2.descDistrito + "->" + x._2.spainMen))

  //statistics and numerics sparks RDD values
  val spainMaleValeByDistrict = grouppedData.map(tupla => tupla._2.spainMen.doubleValue()).cache()
  val media = spainMaleValeByDistrict.mean()
  val stddev = spainMaleValeByDistrict.stdev()
  val max = spainMaleValeByDistrict.max()
  val min = spainMaleValeByDistrict.min()

  println("Media de Españoles varones por distrito: " + media.toInt)
  println("Desviación estandar de españoles varones por distrito: " + stddev.toInt)
  println("Num maximo de españoles varones en un distrito: " + max.toInt)
  println("Num minimo de españoles varones en un distrito: " + min.toInt)


  //sorted list total number of people by district
  grouppedData.collect().sortBy(_._2.numberOfPeople)
    .foreach(x => println(x._2.descDistrito + "->" + x._2.numberOfPeople))

  val numberOfPeopleByDistrict = grouppedData.map(tupla => tupla._2.numberOfPeople.doubleValue).cache()
  val media2 = numberOfPeopleByDistrict.mean()
  val stddev2 = numberOfPeopleByDistrict.stdev()
  val max2 = numberOfPeopleByDistrict.max()
  val min2 = numberOfPeopleByDistrict.min()

  println("Media de personas por distrito: " + media2.toInt)
  println("Desviación estandar de personas por distrito: " + stddev2.toInt)
  println("Num maximo de personas en un distrito: " + max2.toInt)
  println("Num minimo de personas en un distrito: " + min2.toInt)

  def getIntValue(s:String) : Integer = {
    val value = s.substring(1, s.length - 1)
    if (value.isEmpty)
      return 0
    return Integer.parseInt(value)
  }

}