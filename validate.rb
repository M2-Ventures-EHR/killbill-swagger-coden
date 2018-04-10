 #!/usr/bin/env ruby
 
 #
 # USE: > ruby validate.rb "PATH_TO/kbswagger.yaml"
 #
 
 require 'yaml'

 YAML_FILE=ARGV[0]

 puts "Running validation with schema #{YAML_FILE}"
 
 def create_ordered_map(input)
   input.each_with_index.inject({}) {|val, p| val[p[0]] = p[1]; val}
 end

PARAM_TYPE_ORDERING = create_ordered_map(['path', 'body', 'query', 'header'])
PARAM_TYPE_QUERY_NAME_ORDERING = create_ordered_map(['controlPluginName', 'pluginProperty', 'audit'])
PARAM_TYPE_HEADER_NAME_ORDERING = create_ordered_map(['X-Killbill-CreatedBy', 'X-Killbill-Reason', 'X-Killbill-Comment', 'X-Killbill-ApiKey', 'X-Killbill-ApiSecret'])
  
 def validate_arguments(paths, errors)
   paths.each do |pathname, endpoints|
     endpoints.each do |endpoint, endpoint_desc|
       parameters = endpoint_desc["parameters"]

       #puts "\n******  pathname=#{pathname}, endpoint=#{endpoint} ******"        

       cur_type=nil
       cur_qname=nil
       cur_hname=nil

       parameters.each_with_index do |p, idx|

         #puts "    param=#{p}"
         # Validate type ordering
         cur_type = p["in"] if idx == 0
         type = p["in"]
         if cur_type && PARAM_TYPE_ORDERING[type] < PARAM_TYPE_ORDERING[cur_type]
           errors << "Parameter Type Validation Error: pathname=#{pathname}, endpoint=#{endpoint}, param=#{p['name']}: type = #{type} should not come before type #{cur_type}"        
         end
         cur_type = type
         
         # Validate qname ordering
         if type == 'query'
           cur_qname = p['name'] if cur_qname.nil? && PARAM_TYPE_QUERY_NAME_ORDERING[p['name']]
           
           qname = p['name']
           if cur_qname && PARAM_TYPE_QUERY_NAME_ORDERING[qname]             


             if PARAM_TYPE_QUERY_NAME_ORDERING[qname] < PARAM_TYPE_QUERY_NAME_ORDERING[cur_qname]
               errors << "Parameter Query Name Validation Error: pathname=#{pathname}, endpoint=#{endpoint}, param=#{p['name']}: #{qname} should not come before #{cur_qname}"        
             end
             cur_qname = qname
           end
         end

         if type == 'header'
           cur_hname = p['name'] if cur_hname.nil? && PARAM_TYPE_HEADER_NAME_ORDERING[p['name']]
           hname = p['name']

           if cur_hname && PARAM_TYPE_HEADER_NAME_ORDERING[hname]             
             if PARAM_TYPE_HEADER_NAME_ORDERING[hname] < PARAM_TYPE_HEADER_NAME_ORDERING[cur_hname]
               errors << "Parameter Header Name Validation Error: pathname=#{pathname}, endpoint=#{endpoint}, param=#{p['name']}: #{hname} should not come before #{cur_hname}"        
             end
             cur_hname = hname
           end
         end
       end
     end
   end
 end
 
 
YML = YAML.load_file(YAML_FILE)

ERRORS = []
validate_arguments(YML['paths'], ERRORS)

if ERRORS.empty?
  puts "*****  Validation succesfull!! *****  "
else
  puts "*****  Validation Failed *****"
  ERRORS.each do |e|
    puts e
  end  
end   
 
