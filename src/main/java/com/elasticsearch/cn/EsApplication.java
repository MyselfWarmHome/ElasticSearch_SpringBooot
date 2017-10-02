package com.elasticsearch.cn;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.search.MultiMatchQuery;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@SpringBootApplication
@RestController
public class EsApplication {

	@Autowired
	private TransportClient client;

	@GetMapping("/")
	public String index(){return "index";}

	/**
	 * 根据文档数据的id查询数据
	 * @param id 要查询文档的id
	 * @return ResponseEntity的json格式
	 */
	@GetMapping("/get/book/novel")
	@ResponseBody
	public ResponseEntity get(@RequestParam(name="id",defaultValue = "") String id){

		if(id.isEmpty()){
			return new ResponseEntity(HttpStatus.NOT_FOUND);
		}

		GetResponse result = this.client.prepareGet("book","novel",id)
				.get();
		if(!result.isExists()){
			return new ResponseEntity(HttpStatus.NOT_FOUND);
		}

		return new ResponseEntity(result.getSource(),HttpStatus.OK);
	}

	/**
	 * 文档数据的添加
	 * @param title 文档的标题
	 * @param author 作者
	 * @param word_count 字数
	 * @param publish_date 出版时间
	 * @return ResponseEntity的json格式
	 */
	@PostMapping("/add/book/novel")
	@ResponseBody
	public ResponseEntity add(
			@RequestParam(name = "title") String title,
			@RequestParam(name = "author") String author,
			@RequestParam(name = "word_count") String word_count,
			@RequestParam(name = "publish_date")
					@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
					Date publish_date

	){
		try {
			//使用XContentBuilder创建json类型的数据
			XContentBuilder content = XContentFactory.jsonBuilder()
                    .startObject()
                    .field("title",title)
                    .field("author",author)
                    .field("word_count",word_count)
                    .field("publish_date",publish_date.getTime())
                    .endObject();
			//将json数据插入到elasticsearch中
			IndexResponse result = this.client.prepareIndex("book", "novel")
					.setSource(content)
					.get();

			return new ResponseEntity(result.getId(),HttpStatus.OK);
		} catch (IOException e) {
			e.printStackTrace();
			return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 删除指定ID的文档信息
	 * @param id 文档的id
	 * @return ResponseEntity的json格式
	 */
	@DeleteMapping("delete/book/novel")
	@ResponseBody
	public ResponseEntity delete(@RequestParam(name = "id") String id){

		DeleteResponse result = this.client.prepareDelete("book", "novel", id)
				.get();

		return new ResponseEntity(result.getResult().toString(),HttpStatus.OK);
	}

	/**
	 * 文档的更新
	 * @param id 文档id
	 * @param title 文档的标题
	 * @param author 文档的作者
	 * @param word_count 文档的字数
	 * @param publish_date 文档的出版日期
	 * @return ResponseEntity的json格式
	 */
	@PutMapping("update/book/novel")
	@ResponseBody
	public ResponseEntity update(
			@RequestParam(name = "id") String id,
			@RequestParam(name = "title" ,required = false) String title,
			@RequestParam(name = "author" ,required = false) String author,
			@RequestParam(name = "word_count" ,required = false) String word_count,
			@RequestParam(name = "publish_date" ,required = false)
					@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
					String publish_date
	){
		UpdateRequest update = new UpdateRequest("book","novel",id);
		try {
			XContentBuilder builder = XContentFactory.jsonBuilder()
					.startObject();
			if(title != null) {
				builder.field("title", title);
			}

			if(author != null){
				builder.field("author", author);
			}
			
			if(word_count != null){
				builder.field("word_count", word_count);
			}
			
			if(publish_date != null){
				builder.field("publish_date", publish_date);
			}

			builder.endObject();

			update.doc(builder);//将要更新的数据加入到update请求体中

		}catch (IOException e){
			return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		try {
			UpdateResponse result = this.client.update(update).get();
			return new ResponseEntity(result.getGetResult().toString(),HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("query/book/novel")
	@ResponseBody
	public ResponseEntity query(
			@RequestParam(name = "author",required = false) String author,
			@RequestParam(name = "title",required = false) String title,
			@RequestParam(name = "gt_word_count",defaultValue = "0") int gtWordCount,
			@RequestParam(name = "lt_word_count",required = false) Integer ltWordCount

	){
		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

		if(author != null){
			boolQuery.must(QueryBuilders.matchQuery("author",author));
		}
		if(title != null){
			boolQuery.must(QueryBuilders.matchQuery("title",title));
		}

		RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("word_count")
				.from(gtWordCount);

		if(ltWordCount != null && ltWordCount > 0){
			rangeQuery.to(ltWordCount);
		}

		boolQuery.filter(rangeQuery);

		SearchRequestBuilder builder = this.client.prepareSearch("book")
				.setTypes("novel")
				.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
				.setQuery(boolQuery)
				.setFrom(0)
				.setSize(10);

		System.out.println(builder);//请求体格式的显示

		SearchResponse response = builder.get();
		List<Map<String,Object>> result = new ArrayList<>();

		for (SearchHit hit : response.getHits()) {
			result.add(hit.getSource());
		}
		return  new ResponseEntity(result,HttpStatus.OK);
	}


	public static void main(String[] args) {
		SpringApplication.run(EsApplication.class, args);
	}
}
