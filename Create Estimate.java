
// Find all the information about the current project
project_id = project.get("project_id");
project_name = project.get("project_name");
org_id = organization.get("organization_id");
project_record = zoho.books.getRecordsByID("Projects",org_id,project_id);
project_code = project_record.get("code");
// Default tax for all of the line items (15%)
tax_id = "109538000000566001";

if(project_code != 0)
{
	// 14 means fail, 0 means success 
	// if the pull fails, retry
	// It still can manage to fail with this block in place and I am unsure on what the best way is to get it to pass consistantly
	project_record = '';
	project_record = zoho.books.getRecordsByID("Projects",org_id,project_id);
}

// Find the specific key that we want from the map
project_information = project_record.get("project");
project_users = project_information.get("users");

// Project head information for calculation down the line
project_head_name = project_information.get("project_head_name");
project_head_rate = 0;
project_total_hours = project_information.get("total_hours");

// Relavant information for creating the estiamte
customer_id = project.get("customer_id");
customer_name = project.get("customer_name");
project_description = project.get("description");

//
// Building the estimate
//
estimate_map = Map(); // Initialize main map

if(project_description.len() >= 100)
{
	// If the project description is greater than 100 words it throws an error, this is to stop that
	project_description = "";
}
estimate_map.put({"customer_id":customer_id,"customer_name":customer_name,"description":project_description});

//
// line_items
//
// Creates a list to add required information into
user_list = list();
// Find the relavent user information, calculates total cost of each user and appends to list
for each  user in project_users
{
	// Get project head rate for later use
	if(user.get("user_name") == project_head_name)
	{
		project_head_rate = user.get("rate");
	}
	// Get each user within the list
	user_name = user.get("user_name");
	user_hours = user.get("total_hours");
	user_rate = user.get("rate");
	user_total_cost = user_hours;
	// 	Format user_hours to be used in calculation
	user_hours_format = user_hours.replaceFirst(":",".");
	user_hours_format = toDecimal(user_hours_format);
	user_total_cost = user_rate * user_hours_format;
	user_total_cost = user_total_cost.toLong().toDecimal();
	// Append information to list as a map
	temp = {"name":user_name,"quantity":user_hours_format,"rate":user_rate,"amount":user_total_cost, "tax_id": tax_id};
	user_list.add(temp);
}

line_items_list = list();

// Find the Discovery cost
discovery_cost = 0;
for each user in user_list
{
	// Sum total cost for all users
	discovery_cost = discovery_cost + user.get("amount");
}

// Append Discovery information to list
// user_list.add({"name":"Discovery","quantity":1,"rate":discovery_cost,"amount":discovery_cost});
// 'name': item name, 'quantity': hours, 'rate': hourly rate, 'amount': total amount, 'tax_id': specific tax type
line_items_list.add({"name":"Discovery","quantity":1,"rate":discovery_cost,"amount":discovery_cost, "tax_id": tax_id});


// Finds Overheads and Studio Management cost
overheads_studio_management = discovery_cost * 0.05;
// user_list.add({"name":"Overheads and Studio Management","quantity":1,"rate":overheads_studio_management,"amount":overheads_studio_management});
// 'name': item name, 'quantity': hours, 'rate': hourly rate, 'amount'
line_items_list.add({"name":"Overheads and Studio Management","quantity":1,"rate":overheads_studio_management,"amount":overheads_studio_management, "tax_id": tax_id});


// Finds Project Management cost
// Formatting to make the numbers usable for calculation
project_total_hours = project_total_hours.replaceFirst(":",".");
project_total_hours = project_total_hours.toDecimal();
project_management_hours = round(project_total_hours * 0.15,0);

// The quantity is 15% of the total hours rounded to the nearest whole number
project_management_hours = project_management_hours.toNumber();
project_management_total_cost = project_management_hours * project_head_rate;

// Total amount charged
// Append the information to list
// user_list.add({"name":"Project Management","quantity":project_management_hours,"rate":project_head_rate,"amount":project_management_total_cost});
// 'name': item name, 'quantity': hours, 'rate': hourly rate, 'amount'
line_items_list.add({"name":"Project Management","quantity":project_management_hours,"rate":project_head_rate,"amount":project_management_total_cost, "tax_id": tax_id});


// Create map for return statement and append the list we built
// estimate_map.put("line_items",user_list);
estimate_map.put("line_items",line_items_list);

// Project
// "project_name", "project_id", "customer_id", "customer_name", "description", "status", "billing_type"
project_status = project.get("status");
billing_type = project.get("billing_type");


// Add info to map
estimate_map.put("project_name",project_name);
estimate_map.put("project_id",project_id);
estimate_map.put("description",project_description);

// Create and Send the Estimate
// zoho.books.createRecord('Estimates', org_id, estimate_map);
zoho.books.createRecord('Estimates', org_id, estimate_map);

return {"message":"The estimate has been created sucessfully"};
