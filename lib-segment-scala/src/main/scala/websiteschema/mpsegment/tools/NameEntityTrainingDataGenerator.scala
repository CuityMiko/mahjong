package websiteschema.mpsegment.tools

import websiteschema.mpsegment.filter.NameEntityRecognizerBuilder
import websiteschema.mpsegment.util.FileUtil._
import websiteschema.mpsegment.core.SegmentResult
import websiteschema.mpsegment.dict.{ChNameDictionary, POSUtil}
import websiteschema.mpsegment.util.SerializeHandler
import java.io.{PrintWriter, File}
import websiteschema.mpsegment.pinyin.WordToPinyinClassfierFactory
import websiteschema.mpsegment.conf.MPSegmentConfiguration

object NameEntityTrainingDataGenerator extends App {
  val resource = "./src/test/resources/PFR-199801-utf-8.txt"
  val builder = new NameEntityRecognizerBuilder()
  builder.load(resource)
  val result = builder.analysis
  val nameDistribution = new NameProbDistribution()
  val chNameDict = new ChNameDictionary()
  chNameDict.loadNameDict(MPSegmentConfiguration().getChNameDict())

  val loader = PFRCorpusLoader(getResourceAsStream(resource))
  val printer = new PrintWriter(new File("ner.arff"), "utf-8")

  loader.load {
    sentence: SegmentResult => {
//      println(sentence)
      var i = 0
      while (i < sentence.length()) {
        if (sentence.getPOS(i) == POSUtil.POS_NR) {
          val features = new NERFeatures(sentence, i)
          printer.println(i + " " + features.name.mkString + " " +features.getFeatures)
          i = features.nameEntityEndAt
        }else if(sentence.getWord(i).length == 1 && chNameDict.isXing(sentence.getWord(i))) {
          val features = new NERFeatures(sentence, i)
          printer.println(i + " " + features.name.mkString + " " +features.getFeatures)
          i = features.nameEntityEndAt
        } else {
          i += 1
        }
      }
    }
  }

  class NameProbDistribution {

    val loader = SerializeHandler(new File("ner_cn.dat"), SerializeHandler.MODE_READ_ONLY)
    val wordCount = loader.deserializeInt()
    val pinyinFreq = loader.deserializeMapStringInt()
    val wordFreq = loader.deserializeMapStringInt()

    def getProbAsName(words: List[String]): Double = {
      val prob = (words.map(word => if(wordFreq.containsKey(word)) wordFreq.get(word) else 1).sum.toDouble) / wordCount.toDouble
      Math.log(prob) / Math.log(2)
    }

    def getPinyinProbAsName(words: List[String]): Double = {
      val pinyinList = WordToPinyinClassfierFactory().getClassifier().classify(words.mkString)
      val prob = (pinyinList.map(pinyin => if(pinyinFreq.containsKey(pinyin)) pinyinFreq.get(pinyin) else pinyinFreq.get("unknown")).sum.toDouble) / wordCount.toDouble
      Math.log(prob) / Math.log(2)
    }
  }

  class NERFeatures(sentence: SegmentResult, index: Int) {

    val leftBound = if (index == 0) "\0" else sentence.getWord(index - 1)
    val rightBound = if (nameEntityEndAt < sentence.length()) sentence.getWord(nameEntityEndAt) else "\0"
    val isWord = sentence.getPOS(index) == POSUtil.POS_NR

    def getFeatures = {
      List[Double](
        result.leftBoundaryMutualInformation(leftBound),
        result.rightBoundaryMutualInformation(rightBound),
        result.conditionProbability(name),
        result.mutualInformation(name),
        nameDistribution.getProbAsName(name),
//        nameDistribution.getPinyinProbAsName(name),
        if(isWord) 1.0 else 0.0
      )
    }

    def nameEntityEndAt: Int = {
      if (isWord){
        val i = sentence.indexWhere(word => word.pos != POSUtil.POS_NR, index)
        if (i >= 0) i else sentence.length()
      } else {
        if (index < sentence.length() - 3) {
          if ((sentence.getWord(index).length + sentence.getWord(index + 1).length) > 3) {
            index + 1
          } else {
            index + 2
          }
        } else {
          sentence.length()
        }
      }
    }

    def name = {
      val words = for (i <- index until nameEntityEndAt) yield sentence.getWord(i)
      words.mkString.toList.map(ch => ch.toString)
    }
  }

}
