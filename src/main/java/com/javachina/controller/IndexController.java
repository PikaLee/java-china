package com.javachina.controller;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.blade.Blade;
import com.blade.ioc.annotation.Inject;
import com.blade.jdbc.Page;
import com.blade.jdbc.QueryParam;
import com.blade.route.annotation.Path;
import com.blade.route.annotation.PathVariable;
import com.blade.route.annotation.Route;
import com.blade.view.ModelAndView;
import com.blade.web.http.HttpMethod;
import com.blade.web.http.Request;
import com.blade.web.http.Response;
import com.blade.web.multipart.FileItem;
import com.javachina.Constant;
import com.javachina.kit.ImageKit;
import com.javachina.kit.SessionKit;
import com.javachina.model.LoginUser;
import com.javachina.model.Node;
import com.javachina.service.NodeService;
import com.javachina.service.TopicService;

import blade.kit.DateKit;
import blade.kit.FileKit;
import blade.kit.PatternKit;
import blade.kit.StringKit;
import blade.kit.json.JSONObject;

@Path("/")
public class IndexController extends BaseController {

	@Inject
	private TopicService topicService;
	
	@Inject
	private NodeService nodeService;
	
	/**
	 * 首页
	 */
	@Route(value = "/", method = HttpMethod.GET)
	public ModelAndView show_home(Request request, Response response){
		
		this.putData(request, null);
		
		// 最热帖子
		QueryParam hp = QueryParam.me();
		hp.eq("status", 1)/*.between("update_time", start_time, end_time)*/.orderby("tid, comments, views desc").add("limit 10");
		List<Map<String, Object>> hot_topics = topicService.getTopicList(hp);
		request.attribute("hot_topics", hot_topics);
		
		// 最热门的10个节点
		QueryParam np = QueryParam.me();
		np.eq("is_del", 0).notEq("pid", 0).orderby("topics desc").add("limit 10");
		List<Node> hot_nodes = nodeService.getNodeList(np);
		request.attribute("hot_nodes", hot_nodes);
		
		return this.getView("home");
	}
	
	private void putData(Request request, Long nid){
		
		// 帖子
		QueryParam tp = QueryParam.me();
		
		String tab = request.query("tab");
		Integer page = request.queryAsInt("p");
		
		if(StringKit.isNotBlank(tab)){
			QueryParam np = QueryParam.me();
			np.eq("is_del", 0).eq("slug", tab);
			Node node = nodeService.getNode(np);
			if(null != node){
				tp.eq("nid", node.getNid());
				request.attribute("tab", tab);
				request.attribute("node_name", node.getTitle());
			}
		} else {
			if(null != nid){
				tp.eq("nid", nid);
			}
		}
		
		if(null == page || page < 1){
			page = 1;
		}
		
		tp.eq("status", 1).orderby("update_time desc").page(page, 15);
		Page<Map<String, Object>> topicPage = topicService.getPageList(tp);
		request.attribute("topicPage", topicPage);
		
		// 读取节点列表
		List<Map<String, Object>> nodes = nodeService.getNodeList();
		request.attribute("nodes", nodes);
	}
	
	/**
	 * 节点主题页
	 */
	@Route(value = "/go/:slug", method = HttpMethod.GET)
	public ModelAndView go(@PathVariable("slug") String slug,
			Request request, Response response){
		
		QueryParam np = QueryParam.me();
		np.eq("is_del", 0).eq("slug", slug);
		
		Node node = nodeService.getNode(np);
		if(null == node){
			// 不存在的节点
			response.text("not found node.");
			return null;
		}
		
		this.putData(request, node.getNid());
		
		Map<String, Object> nodeMap = nodeService.getNodeDetail(null, node.getNid());
		request.attribute("node", nodeMap);
		
		return this.getView("node_detail");
	}
	
	
	/**
	 * 上传头像
	 */
	@Route(value = "/uploadimg", method = HttpMethod.POST)
	public void uploadimg(Request request, Response response){
		LoginUser user = SessionKit.getLoginUser();
		if(null == user){
			return;
		}
		FileItem[] fileItems = request.files();
		if(null != fileItems && fileItems.length > 0){
			
			FileItem fileItem = fileItems[0];
			
			String type = request.query("type");
			String suffix = FileKit.getExtension(fileItem.getFileName());
			if(StringKit.isNotBlank(suffix)){
				suffix = "." + suffix;
			}
			if(!PatternKit.isImage(suffix)){
				return;
			}
			
			if(null == type){
				type = "temp";
			}
			
			String saveName = DateKit.dateFormat(new Date(), "yyyyMMddHHmmssSSS")  + "_" + StringKit.getRandomChar(10) + suffix;
			File file = new File(Blade.me().webRoot() + File.separator + Constant.UPLOAD_FOLDER + File.separator + saveName);
			
			try {
				
				ImageKit.copyFileUsingFileChannels(fileItem.getFile(), file);
				
				String filePath = Constant.UPLOAD_FOLDER + "/" + saveName;
				
				JSONObject res = new JSONObject();
				res.put("status", 200);
				res.put("savekey", filePath);
				res.put("savepath", filePath);
				res.put("url", Constant.SITE_URL + "/" + filePath);
				
				response.json(res.toString());
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}
	
	/**
	 * markdown页面
	 */
	@Route(value = "/markdown", method = HttpMethod.GET)
	public ModelAndView markdown(Request request, Response response){
		return this.getView("markdown");
	}
	
}
