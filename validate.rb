 #!/usr/bin/env ruby
 
 #
 # USE: > ruby validate.rb "PATH_TO/kbswagger.yaml"
 #
 
 require 'yaml'

 YAML_FILE=ARGV[0]

# puts "Running validation with schema #{YAML_FILE}"
 
 def create_ordered_map(input)
   input.each_with_index.inject({}) {|val, p| val[p[0]] = p[1]; val}
 end

PARAM_TYPE_ORDERING = create_ordered_map(['path', 'body', 'query', 'header'])
PARAM_TYPE_QUERY_NAME_ORDERING = create_ordered_map(['controlPluginName', 'pluginProperty', 'audit'])
PARAM_TYPE_HEADER_NAME_ORDERING = create_ordered_map(['X-Killbill-CreatedBy', 'X-Killbill-Reason', 'X-Killbill-Comment', 'X-Killbill-ApiKey', 'X-Killbill-ApiSecret'])

RE_SUCCESS=/^2\d+/
SUCCESS_RESPONSES = { 'post' => [200, 201, 202, 204], 'get' => [200],  'put' => [204],  'delete' => [204]}

# TODO Decide what is acceptable or not see https://github.com/killbill/killbill/issues/927
PRODUCES_RESPONSES = { 'post' => ['consumes'], 'get' => ['produces'],  'put' => ['consumes'],  'delete' => ['consumes']}




def validate_arguments(paths, validation)
   
   err = validation['err']
   #ok = validation['ok']
      
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
           err[endpoint] <<  "Parameter Type Validation Error: pathname=#{pathname}, endpoint=#{endpoint}, param=#{p['name']}: type = #{type} should not come before type #{cur_type}"        
         end
         cur_type = type
         
         # Validate qname ordering
         if type == 'query'
           cur_qname = p['name'] if cur_qname.nil? && PARAM_TYPE_QUERY_NAME_ORDERING[p['name']]
           
           qname = p['name']
           if cur_qname && PARAM_TYPE_QUERY_NAME_ORDERING[qname]             


             if PARAM_TYPE_QUERY_NAME_ORDERING[qname] < PARAM_TYPE_QUERY_NAME_ORDERING[cur_qname]
               err[endpoint] <<  "Parameter Query Name Validation Error: pathname=#{pathname}, endpoint=#{endpoint}, param=#{p['name']}: #{qname} should not come before #{cur_qname}"        
             end
             cur_qname = qname
           end
         end

         if type == 'header'
           cur_hname = p['name'] if cur_hname.nil? && PARAM_TYPE_HEADER_NAME_ORDERING[p['name']]
           hname = p['name']

           if cur_hname && PARAM_TYPE_HEADER_NAME_ORDERING[hname]             
             if PARAM_TYPE_HEADER_NAME_ORDERING[hname] < PARAM_TYPE_HEADER_NAME_ORDERING[cur_hname]
               err[endpoint] <<  "Parameter Header Name Validation Error: pathname=#{pathname}, endpoint=#{endpoint}, param=#{p['name']}: #{hname} should not come before #{cur_hname}"        
             end
             cur_hname = hname
           end
         end
       end
     end
   end
end

def validate_accept_and_content_type_headers(paths, validation)
  
  err = validation['err']
  ok = validation['ok']
  

  paths.each do |pathname, endpoints|
    endpoints.each do |endpoint, endpoint_desc|

      ok[endpoint]['produces'] = {} unless ok[endpoint].key? 'produces'
      ok[endpoint]['consumes'] = {} unless ok[endpoint].key? 'consumes'

      produces = endpoint_desc['produces']
      consumes = endpoint_desc['consumes']
      
      if produces && !produces.empty?
        produces.each do |p|
          ok[endpoint]['produces'][p] = [] unless ok[endpoint]['produces'].key?(p)
          ok[endpoint]['produces'][p] << "#{pathname.split('/')[3] }##{endpoint_desc['operationId']}"
        end
      end

      if consumes && !consumes.empty?
        consumes.each do |p|
          ok[endpoint]['consumes'][p] = [] unless ok[endpoint]['consumes'].key?(p)
          ok[endpoint]['consumes'][p] << "#{pathname.split('/')[3] }##{endpoint_desc['operationId']}"
        end
      end
    end
  end  
end

 
def validate_response_status(paths, validation)
  
  err = validation['err']
  ok = validation['ok']
  
  paths.each do |pathname, endpoints|
    endpoints.each do |endpoint, endpoint_desc|
      responses = endpoint_desc['responses']

      http_success = nil
      found_success = nil
      responses.each do |k, v|   
        http_success = k if http_success.nil? && RE_SUCCESS.match(k.to_s)
        found_success = SUCCESS_RESPONSES[endpoint].include?(k)
        break if found_success
      end
      
      if !found_success
        err[endpoint] << "#{pathname.split('/')[3] }##{endpoint_desc['operationId']}"
      else
        ok[endpoint][http_success] = [] unless ok[endpoint].key?(http_success)
        ok[endpoint][http_success] << "#{pathname.split('/')[3] }##{endpoint_desc['operationId']}"
      end
    end
  end
end   
 
def init_validation()
  err = {'get' => [], 'post' => [], 'put' => [], 'delete' => []}
  ok = {'get' => {}, 'post' => {}, 'put' => {}, 'delete' => {}}
  validation = {}
  validation['err'] = err
  validation['ok'] = ok
  validation
end 
 
def print_all_info(validation)
  puts "\n******** SCHEMA INFO ********"
  print_response_info("OK response status:", validation['response_status']['ok'])
  
  print_consume_produce_info("Produce/consume:", validation['produce_consume']['ok'])
end


def print_response_info(msg, ok)
  puts
  puts "* #{msg}"
  
  ok.keys.each do |endpoint|    
    puts "  #{endpoint}:"
    ok[endpoint].keys.each do |status|
      puts "    - #{status}"
      ok[endpoint][status].each do |e|
        puts "      #{e}"      
      end
    end
  end
end  

def print_consume_produce_info(msg, ok)
  puts
  puts "* #{msg}"
  
  ok.keys.each do |endpoint|    
    puts "  #{endpoint}:"
    puts "    Produce:"  
    ok[endpoint]['produces'].keys.each do |p|
      puts "      - #{p}"
      ok[endpoint]['produces'][p].each do |v|
        puts "          #{v}"
      end
    end
    puts "    Consume:"  
    ok[endpoint]['consumes'].keys.each do |p|
      puts "      - #{p}"
      ok[endpoint]['consumes'][p].each do |v|
        puts "          #{v}"
      end
    end

  end
end  


 
def print_all_errors(validation)
  puts "\n******** VALIDATION ERRORS ********"
  print_errors("Missing ok response status:", validation['response_status']['err'])
  print_errors("Bad argument ordering:", validation['arguments']['err'])  
end
 
def print_errors(msg, err)
  puts
  puts "* #{msg}"
  
  found_err = false
  err.keys.each do |endpoint|
    if ! err[endpoint].empty?
      puts "  #{endpoint}:"
      err[endpoint].each do |e|
        puts "    - #{e}"
        found_err = true
      end
    end
  end  
  puts "No errors" if !found_err
end 
 
YML = YAML.load_file(YAML_FILE)

VALIDATION = {}
VALIDATION['response_status'] = init_validation
VALIDATION['produce_consume'] = init_validation
VALIDATION['arguments'] = init_validation

validate_arguments(YML['paths'], VALIDATION['arguments'] )
validate_response_status(YML['paths'], VALIDATION['response_status'])
validate_accept_and_content_type_headers(YML['paths'], VALIDATION['produce_consume'])

print_all_errors(VALIDATION)
print_all_info(VALIDATION)

