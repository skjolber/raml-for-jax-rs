package com.mulesoft.jaxrs.raml.annotation.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.raml.model.Protocol;
import org.raml.model.Raml2;
import org.raml.model.RamlFileVisitorAdapter;
import org.raml.model.Resource;
import org.raml.model.parameter.UriParameter;

public class RAMLModelHelper {

	protected Raml2 coreRaml = new Raml2();

	public RAMLModelHelper() {
		coreRaml.setBaseUri("http://example.com");
		coreRaml.setTitle("Please type API title here");
		coreRaml.setProtocols(Collections.singletonList(Protocol.HTTP));
	}

	public String getMediaType() {
		return coreRaml.getMediaType();
	}

	public void addResource(Resource res) {
		cleanupUrl(res);
		String relativeUri = res.getRelativeUri();
		int c = 0;
		for (int a = 0; a < relativeUri.length(); a++) {
			if (relativeUri.charAt(a) == '/') {
				c++;
			}
		}
		Map<String, Resource> resources = getCoreRaml().getResources();
		if (c == 1) {
			Resource put = resources.put(relativeUri, res);
			if (put != null) {
				merge(res, put);
			}
			if (relativeUri.length() > 0) {
				Path ps = new Path(relativeUri);
				// lets search for sub resources to gather
				for (String s : new HashSet<String>(resources.keySet())) {
					Path anotherPath = new Path(s);
					if (ps.isPrefixOf(anotherPath) && !ps.equals(anotherPath)
							&& ps.segmentCount() > 0) {
						Resource remove = resources.remove(s);
						Path removeFirstSegments = anotherPath
								.removeFirstSegments(ps.segmentCount());
						String portableString = removeFirstSegments
								.toPortableString();
						String doCleanup = doCleanup("/" + portableString);
						res.getResources().put(doCleanup, remove);
						remove.setRelativeUri(doCleanup);
					}
				}
			}
			return;
		}
		if (res.getRelativeUri().length() == 0 && res.getActions().isEmpty()) {
			return;
		}
		placeResource(resources, res);
	}

	private static void merge(Resource res, Resource put) {
		res.getActions().putAll(put.getActions());
		for (String s : put.getResources().keySet()) {
			if (res.getResources().containsKey(s)) {
				merge(res.getResources().get(s), put.getResources().get(s));
			} else {
				res.getResources().put(s, put.getResources().get(s));
			}
		}
	}

	private void cleanupUrl(Resource res) {

		String relativeUri = res.getRelativeUri();

		String string = doCleanup(relativeUri);
		if (string.length() == 0) {
			string = "/";
		}
		res.setRelativeUri(string);
	}

	private static String doCleanup(String relativeUri) {
		relativeUri = PathCleanuper.cleanupPath(relativeUri);
		StringBuilder bld = new StringBuilder();
		char pc = 0;
		for (int a = 0; a < relativeUri.length(); a++) {
			char c = relativeUri.charAt(a);
			if (c == '/' && pc == '/') {
				continue;
			} else {
				bld.append(c);
				pc = c;
			}
		}
		String string = bld.toString();
		if (!string.startsWith("/")) {
			string = "/" + string;
		}
		return string;
	}

	// TODO More accurate resource merging
	public static void placeResource(Map<String, Resource> resources,
			Resource createResource) {
		String relativeUri = createResource.getRelativeUri();
		Path path = new Path(relativeUri);
		boolean restructure = false;
		for (String s : new HashSet<String>(resources.keySet())) {
			Path rp = new Path(s);
			if (path.isPrefixOf(rp)) {
				if (path.equals(rp)) {
					Resource resource = resources.get(s);
					resource.getActions().putAll(createResource.getActions());
					return;
				}
				restructure = true;
				Resource remove = resources.remove(s);
				Path removeFirstSegments = rp.removeFirstSegments(path
						.segmentCount());
				String portableString = "/"
						+ removeFirstSegments.toPortableString();
				portableString = doCleanup(portableString);
				remove.setRelativeUri(portableString);
				Resource old = resources.put(relativeUri, createResource);
				if (old != null) {
					createResource.getActions().putAll(old.getActions());
				}
				Map<String, UriParameter> uriParameters = createResource
						.getUriParameters();
				Map<String, UriParameter> uriParameters2 = remove
						.getUriParameters();
				for (String q : uriParameters.keySet()) {
					uriParameters2.remove(q);
				}
				createResource.getResources().put(portableString, remove);
				Resource put = resources.put(relativeUri, createResource);
				if (put != null) {
					createResource.getActions().putAll(put.getActions());
				}
			}
		}
		if (restructure) {
			return;
		}
		for (String s : resources.keySet()) {
			if (s.equals("/")) {
				continue;
			}
			Path rp = new Path(s);
			if (rp.isPrefixOf(path)
					&& path.segmentCount() - rp.segmentCount() >= 1) {
				Path removeFirstSegments2 = path.removeFirstSegments(rp
						.segmentCount());
				Path removeFirstSegments = removeFirstSegments2;
				String portableString = "/"
						+ removeFirstSegments.toPortableString();

				createResource.setRelativeUri(doCleanup(portableString));
				Resource resource = resources.get(s);
				Map<String, UriParameter> uriParameters = resource
						.getUriParameters();
				Map<String, UriParameter> uriParameters2 = createResource
						.getUriParameters();
				for (String sa : uriParameters.keySet()) {
					uriParameters2.remove(sa);
				}
				placeResource(resource.getResources(), createResource);
				return;
			}
		}
		Resource put = resources.put(relativeUri, createResource);
		if (put != null) {
			merge(createResource, put);
		}
	}

	public void setMediaType(String mediaType) {
		coreRaml.setMediaType(mediaType);
	}
	

	public Raml2 getCoreRaml() {
		
		return coreRaml;
	}

	public void optimize() {
		optimizeResourceMap(coreRaml.getResources());
		coreRaml.visit(new RamlFileVisitorAdapter() {
			
			public boolean startVisit(Resource resource) {
				optimizeResourceMap(resource.getResources());
				return super.startVisit(resource);
			}
		});
	}

	protected void optimizeResourceMap(Map<String, Resource> resources) {
		sortIfNeeded(resources);
		extractCommonPaths(resources);
	}
	boolean extractCommonParts=true;

	private void extractCommonPaths(Map<String, Resource> resources) {
		if(!extractCommonParts){
			return;
		}
		LinkedHashMap<String, Resource> rs = (LinkedHashMap<String, Resource>) resources;
		ArrayList<Entry> rt = getEntries(rs);
		HashMap<String, ArrayList<Entry>>map=new HashMap<String, ArrayList<Entry>>();
		for (Entry e:rt){
			Path c=new Path(e.path);
			if (c.segmentCount()>1){
				String segment = c.segment(0);
				ArrayList<Entry> arrayList = map.get(segment);
				if (arrayList==null){
					arrayList=new ArrayList<RAMLModelHelper.Entry>();
					map.put(segment, arrayList);
				}
				arrayList.add(e);
			}
		}
		if (!map.isEmpty()){
			//list of entries to collapse segment
			for (String  s:map.keySet()){
				ArrayList<Entry>e=map.get(s);
				Entry base=e.get(0);
				Resource r0=base.res;
				Resource newRes=new Resource();
				String relativeUri = "/"+s;
				newRes.setRelativeUri(relativeUri);
				base.path=relativeUri;
				stripSegment(r0);
				newRes.getResources().put(r0.getRelativeUri(), r0);
				base.res=newRes;
				for (int a=1;a<e.size();a++){
					Entry entry = e.get(a);
					stripSegment(entry.res);
					rt.remove(entry);
					newRes.getResources().put(entry.res.getRelativeUri(), entry.res);
				}				
			}
		}
		resources.clear();
		entriesToMap(resources, rt);
	}

	private void stripSegment(Resource r0) {
		String relativeUri = r0.getRelativeUri();
		Path p=new Path(relativeUri);
		p=p.removeFirstSegments(1);
		r0.setRelativeUri("/"+p.toPortableString());
	}

	private ArrayList<Entry> getEntries(LinkedHashMap<String, Resource> rs) {
		ArrayList<Entry> rt = new ArrayList<RAMLModelHelper.Entry>();
		for (String path : rs.keySet()) {
			rt.add(new Entry(path, rs.get(path)));
		}
		return rt;
	}

	private void entriesToMap(Map<String, Resource> resources,
			ArrayList<Entry> rt) {
		for (Entry e : rt) {
			resources.put(e.path, e.res);
		}
	}

	static class Entry implements Comparable<Entry> {
		protected String path;

		public Entry(String path, Resource res) {
			super();
			this.path = path;
			this.res = res;
		}

		protected Resource res;

		
		public int compareTo(Entry o) {
			return path.compareTo(o.path);
		}
	}

	boolean doSort=true;

	private void sortIfNeeded(Map<String, Resource> resources) {
		if (doSort) {
			LinkedHashMap<String, Resource> rs = (LinkedHashMap<String, Resource>) resources;
			ArrayList<Entry> rt = getEntries(rs);
			Collections.sort(rt);
			resources.clear();
			entriesToMap(resources, rt);
		}
	}

}
