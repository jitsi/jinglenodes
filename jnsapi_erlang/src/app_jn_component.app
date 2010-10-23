{application, app_jn_component,
 [{description, "Yahoo Jingle Relay"},
  {vsn, "1"},
  {modules, [app_jn_component, sup_jn_component, jn_component]},
  {registered, [yrelay]},
  {applications, []},
  {mod, {app_jn_component,[]}}
 ]}.


